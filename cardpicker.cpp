#include <jni.h>
#include <vector>
#include <algorithm>
#include <random>
#include <ctime>

// Called by Java via JNI — picks 3 unique random card indices
extern "C" {

JNIEXPORT jintArray JNICALL
Java_CardPickerNative_pickThree(JNIEnv* env, jobject obj, jint deckSize) {
    // Build a list of all indices then shuffle
    std::vector<int> indices(deckSize);
    for (int i = 0; i < deckSize; i++) indices[i] = i;

    std::mt19937 rng(static_cast<unsigned int>(std::time(nullptr)));
    std::shuffle(indices.begin(), indices.end(), rng);

    // Return the first 3
    jintArray result = env->NewIntArray(3);
    jint picked[3] = { indices[0], indices[1], indices[2] };
    env->SetIntArrayRegion(result, 0, 3, picked);
    return result;
}

} // extern "C"
