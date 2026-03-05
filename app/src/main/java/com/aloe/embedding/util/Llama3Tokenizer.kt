package com.llama.tokenizer

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Llama3 / GPT-4 스타일 ByteLevel BPE 토크나이저
 *
 * HuggingFace tokenizer.json 기반, 순수 Kotlin 구현.
 * .so / JNI 불필요. Android API 21+ 호환.
 *
 * ## 사용법
 * ```kotlin
 * val tokenizer = Llama3Tokenizer.fromAssets(context)
 * val ids: List<Int> = tokenizer.encode("Hello, world!")
 * val text: String  = tokenizer.decode(ids)
 * ```
 *
 * ## Assets 파일 준비 (generate_tokenizer_assets.py 로 자동 생성)
 *   assets/tokenizer_vocab.txt   -> `토큰\t아이디`
 *   assets/tokenizer_merges.txt  -> `토큰A\t토큰B`  (우선순위 순)
 *   assets/tokenizer_special.txt -> `토큰\t아이디`
 */
class Llama3Tokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val idToToken: Map<Int, String>,
    private val mergeRanks: Map<Pair<String, String>, Int>,
    private val addedTokens: Map<String, Int>,
) {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    val vocabSize: Int get() = vocab.size

    /**
     * 텍스트를 토큰 ID 리스트로 인코딩
     * @param addSpecialTokens true면 끝에 <|end_of_text|> 추가
     */
    fun encode(text: String, addSpecialTokens: Boolean = false): List<Int> {
        val ids = mutableListOf<Int>()

        for ((part, isSpecial) in splitBySpecialTokens(text)) {
            if (isSpecial) {
                addedTokens[part]?.let { ids.add(it) }
            } else if (part.isNotEmpty()) {
                for (word in preTokenize(part)) {
                    for (token in applyBpe(word.encodeToByteArray())) {
                        vocab[token]?.let { ids.add(it) }
                    }
                }
            }
        }

        if (addSpecialTokens) {
            addedTokens[TOKEN_EOT]?.let { ids.add(it) }
        }

        return ids
    }

    /**
     * 토큰 ID 리스트를 텍스트로 디코딩
     */
    fun decode(ids: List<Int>): String {
        val byteList = mutableListOf<Byte>()
        val sb = StringBuilder()

        for (id in ids) {
            val token = idToToken[id] ?: continue

            // special token: 먼저 쌓인 바이트 플러시 후 토큰 문자열 추가
            if (addedTokens.containsValue(id)) {
                if (byteList.isNotEmpty()) {
                    sb.append(byteList.toByteArray().toString(Charsets.UTF_8))
                    byteList.clear()
                }
                sb.append(token)
                continue
            }

            // ByteLevel 디코딩
            for (ch in token) {
                UNICODE_TO_BYTE[ch]?.let { byteList.add(it.toByte()) }
            }
        }

        if (byteList.isNotEmpty()) {
            sb.append(byteList.toByteArray().toString(Charsets.UTF_8))
        }

        return sb.toString()
    }

    /**
     * 텍스트를 토큰 문자열 배열로 변환
     * encode("Hello, world!") -> ["Hello", ",", " world", "!"]
     */
    fun tokenize(text: String): Array<String> {
        val tokens = mutableListOf<String>()

        for ((part, isSpecial) in splitBySpecialTokens(text)) {
            if (isSpecial) {
                tokens.add(part)
            } else if (part.isNotEmpty()) {
                for (word in preTokenize(part)) {
                    for (bpeToken in applyBpe(word.encodeToByteArray())) {
                        // ByteLevel -> 실제 문자열로 디코딩
                        val bytes = bpeToken.map { ch ->
                            UNICODE_TO_BYTE[ch]?.toByte() ?: '?'.code.toByte()
                        }.toByteArray()
                        tokens.add(bytes.toString(Charsets.UTF_8))
                    }
                }
            }
        }

        return tokens.toTypedArray()
    }

    fun tokenToId(token: String): Int? = vocab[token] ?: addedTokens[token]
    fun idToString(id: Int): String? = idToToken[id]

    // -------------------------------------------------------------------------
    // Pre-tokenizer
    //
    // GPT-4 / Llama3 regex를 수동 구현:
    //   (?i:'s|'t|'re|'ve|'m|'ll|'d)
    //   | [^\r\n\p{L}\p{N}]?\p{L}+
    //   | \p{N}{1,3}
    //   | ?[^\s\p{L}\p{N}]+[\r\n]*
    //   | \s*[\r\n]+
    //   | \s+(?!\S)
    //   | \s+
    // -------------------------------------------------------------------------

    private fun preTokenize(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        val n = text.length

        while (i < n) {
            val c = text[i]

            // 1) 축약형
            if (c == '\'') {
                var found = false
                for (con in CONTRACTIONS) {
                    val end = i + con.length
                    if (end <= n && text.substring(i, end).lowercase() == con) {
                        result.add(text.substring(i, end))
                        i = end; found = true; break
                    }
                }
                if (found) continue
            }

            // 2) \s*[\r\n]+ : 줄바꿈
            if (c == '\r' || c == '\n') {
                val start = i
                while (i < n && (text[i] == '\r' || text[i] == '\n')) i++
                result.add(text.substring(start, i)); continue
            }

            // 3) 공백으로 시작하는 패턴
            if (c == ' ') {
                var j = i
                while (j < n && text[j] == ' ') j++

                when {
                    // 공백 + 줄바꿈
                    j < n && (text[j] == '\r' || text[j] == '\n') -> {
                        val start = i; i = j
                        while (i < n && (text[i] == '\r' || text[i] == '\n')) i++
                        result.add(text.substring(start, i))
                    }
                    // 공백 + 문자 -> [^\r\n\p{L}\p{N}]?\p{L}+
                    j < n && isLetter(text[j]) -> {
                        val sb = StringBuilder(text.substring(i, j))
                        i = j
                        while (i < n && isLetter(text[i])) sb.append(text[i++])
                        result.add(sb.toString())
                    }
                    // 공백 + 구두점 -> ?[^\s\p{L}\p{N}]+[\r\n]*
                    j < n && !isLetter(text[j]) && !isNum(text[j]) && !text[j].isWhitespace() -> {
                        val sb = StringBuilder(" ")
                        i = j
                        while (i < n && !text[i].isWhitespace() && !isLetter(text[i]) && !isNum(text[i])) {
                            sb.append(text[i++])
                        }
                        while (i < n && (text[i] == '\r' || text[i] == '\n')) sb.append(text[i++])
                        result.add(sb.toString())
                    }
                    // 나머지 공백 (숫자 앞, trailing 등)
                    else -> {
                        result.add(text.substring(i, j)); i = j
                    }
                }
                continue
            }

            // 4) 비공백 단일 prefix + 문자
            if (!isLetter(c) && !isNum(c) && !c.isWhitespace()) {
                if (i + 1 < n && isLetter(text[i + 1])) {
                    val sb = StringBuilder().append(c); i++
                    while (i < n && isLetter(text[i])) sb.append(text[i++])
                    result.add(sb.toString()); continue
                }
            }

            // 5) 문자
            if (isLetter(c)) {
                val start = i
                while (i < n && isLetter(text[i])) i++
                result.add(text.substring(start, i)); continue
            }

            // 6) 숫자 (최대 3자리)
            if (isNum(c)) {
                val start = i;
                var count = 0
                while (i < n && isNum(text[i]) && count < 3) {
                    i++; count++
                }
                result.add(text.substring(start, i)); continue
            }

            // 7) 구두점 등
            if (!c.isWhitespace() && !isLetter(c) && !isNum(c)) {
                val start = i
                while (i < n && !text[i].isWhitespace() && !isLetter(text[i]) && !isNum(text[i])) i++
                while (i < n && (text[i] == '\r' || text[i] == '\n')) i++
                result.add(text.substring(start, i)); continue
            }

            result.add(c.toString()); i++
        }

        return result
    }

    // -------------------------------------------------------------------------
    // ByteLevel BPE
    // -------------------------------------------------------------------------

    private fun applyBpe(bytes: ByteArray): List<String> {
        if (bytes.isEmpty()) return emptyList()

        var word = Array(bytes.size) { idx ->
            BYTE_TO_UNICODE[bytes[idx].toInt() and 0xFF].toString()
        }

        if (word.size == 1) return word.toList()

        while (word.size > 1) {
            var bestRank = Int.MAX_VALUE
            var bestIdx = -1

            for (i in 0 until word.size - 1) {
                val rank = mergeRanks[Pair(word[i], word[i + 1])] ?: Int.MAX_VALUE
                if (rank < bestRank) {
                    bestRank = rank; bestIdx = i
                }
            }

            if (bestIdx == -1 || bestRank == Int.MAX_VALUE) break

            val merged = word[bestIdx] + word[bestIdx + 1]
            word = Array(word.size - 1) { idx ->
                when {
                    idx < bestIdx -> word[idx]
                    idx == bestIdx -> merged
                    else -> word[idx + 1]
                }
            }
        }

        return word.toList()
    }

    // -------------------------------------------------------------------------
    // Special token 분리
    // -------------------------------------------------------------------------

    private fun splitBySpecialTokens(text: String): List<Pair<String, Boolean>> {
        if (addedTokens.isEmpty()) return listOf(Pair(text, false))

        val result = mutableListOf<Pair<String, Boolean>>()
        val sortedTokens = addedTokens.keys.sortedByDescending { it.length }
        var pos = 0

        while (pos < text.length) {
            var earliest = -1
            var earliestToken = ""

            for (token in sortedTokens) {
                val idx = text.indexOf(token, pos)
                if (idx != -1 && (earliest == -1 || idx < earliest)) {
                    earliest = idx; earliestToken = token
                }
            }

            if (earliest == -1) {
                result.add(Pair(text.substring(pos), false)); break
            }

            if (earliest > pos) result.add(Pair(text.substring(pos, earliest), false))
            result.add(Pair(earliestToken, true))
            pos = earliest + earliestToken.length
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Unicode helpers
    // -------------------------------------------------------------------------

    private fun isLetter(c: Char): Boolean {
        val t = Character.getType(c)
        return t == Character.UPPERCASE_LETTER.toInt() ||
                t == Character.LOWERCASE_LETTER.toInt() ||
                t == Character.TITLECASE_LETTER.toInt() ||
                t == Character.MODIFIER_LETTER.toInt() ||
                t == Character.OTHER_LETTER.toInt()
    }

    private fun isNum(c: Char): Boolean {
        val t = Character.getType(c)
        return t == Character.DECIMAL_DIGIT_NUMBER.toInt() ||
                t == Character.LETTER_NUMBER.toInt() ||
                t == Character.OTHER_NUMBER.toInt() ||
                c.isDigit()
    }

    // -------------------------------------------------------------------------
    // Companion: ByteLevel 테이블 & 팩토리
    // -------------------------------------------------------------------------

    companion object {
        private const val TOKEN_EOT = "<|end_of_text|>"
        private val CONTRACTIONS = listOf("'ll", "'re", "'ve", "'m", "'d", "'s", "'t")

        /**
         * GPT-2 / Llama3 ByteLevel 매핑: byte(0-255) -> unicode char
         */
        val BYTE_TO_UNICODE: CharArray by lazy {
            val result = CharArray(256)

            // printable 범위는 그대로 매핑
            for (b in '!'.code..'~'.code) result[b] = b.toChar()
            for (b in '¡'.code..'¬'.code) result[b] = b.toChar()
            for (b in '®'.code..'ÿ'.code) result[b] = b.toChar()

            // 나머지는 256부터 순서대로
            var n = 0
            for (b in 0..255) {
                if (result[b] == '\u0000') {
                    result[b] = (256 + n).toChar(); n++
                }
            }
            result
        }

        val UNICODE_TO_BYTE: Map<Char, Int> by lazy {
            BYTE_TO_UNICODE.mapIndexed { byte, char -> char to byte }.toMap()
        }

        // ------------------------------------------------------------------
        // 팩토리
        // ------------------------------------------------------------------

        /**
         * Android Assets에서 로딩 (권장)
         */
        fun fromAssets(context: Context): Llama3Tokenizer = fromStreams(
            vocabStream = context.assets.open("tokenizer_vocab.txt"),
            mergesStream = context.assets.open("tokenizer_merges.txt"),
            specialStream = context.assets.open("tokenizer_special.txt"),
        )

        /**
         * InputStream에서 로딩
         */
        fun fromStreams(
            vocabStream: InputStream,
            mergesStream: InputStream,
            specialStream: InputStream,
        ): Llama3Tokenizer {
            val vocab = HashMap<String, Int>(140_000)
            val idToToken = HashMap<Int, String>(140_000)
            val mergeRanks = HashMap<Pair<String, String>, Int>(300_000)
            val addedTokens = HashMap<String, Int>(300)

            fun InputStream.readLines(block: (String) -> Unit) =
                BufferedReader(
                    InputStreamReader(
                        this,
                        Charsets.UTF_8
                    )
                ).use { it.forEachLine(block) }

            vocabStream.readLines { line ->
                val tab = line.lastIndexOf('\t')
                if (tab > 0) {
                    val token = line.substring(0, tab)
                    val id = line.substring(tab + 1).toInt()
                    vocab[token] = id; idToToken[id] = token
                }
            }

            var rank = 0
            mergesStream.readLines { line ->
                val tab = line.indexOf('\t')
                if (tab > 0) {
                    mergeRanks[Pair(line.substring(0, tab), line.substring(tab + 1))] = rank++
                }
            }

            specialStream.readLines { line ->
                val tab = line.lastIndexOf('\t')
                if (tab > 0) {
                    val token = line.substring(0, tab)
                    val id = line.substring(tab + 1).toInt()
                    addedTokens[token] = id; idToToken[id] = token
                }
            }

            return Llama3Tokenizer(vocab, idToToken, mergeRanks, addedTokens)
        }
    }
}
