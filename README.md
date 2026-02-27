# Embedding App

Android에서 온디바이스 텍스트 임베딩을 실행하는 앱입니다. 
**Gemma**와 **Jina** 두 가지 임베딩 모델을 지원합니다.

## 지원 모델

| 모델 | 설명 |
|------|------|
| Gemma | Google의 Gemma 기반 임베딩 모델 (TFLite) |
| Jina | Jina AI의 ONNX 임베딩 모델 |

## 시작하기

### 1. 모델 다운로드

아래 링크에서 모델 파일을 다운로드하세요.

- **Gemma 모델** (`embeddinggemma.tflite`): [다운로드](https://drive.google.com/file/d/1xodBkrH_U8bMX3Ay33Ctv5ARQYyyUmOE/view?usp=drive_link)
- **Jina 모델** (`model_merged.onnx`): [다운로드](https://drive.google.com/file/d/1n6QmB_TOCloh3ktVEH7V4rBpB1n9zb9l/view?usp=drive_link)

### 2. 모델 파일 배치

다운로드한 파일을 아래 경로에 넣어주세요.
```
app/src/main/assets/
├── embeddinggemma.tflite
├── model_merged.onnx
```

### 3. 빌드 및 실행

Android Studio에서 프로젝트를 열고 실행하세요.
