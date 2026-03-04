use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_com_aloe_embedding_MainActivity_helloRust(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env
        .new_string("Hello from Rust! 🦀")
        .expect("Couldn't create Java string");
    output.into_raw()
}
