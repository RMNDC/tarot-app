// JNI bridge — connects Java to the C++ card picker library
public class CardPickerNative {
    static { System.loadLibrary("cardpicker"); }

    // This method is implemented in cardpicker.cpp
    public native int[] pickThree(int deckSize);
}
