package com.caiotcruz.mygamelist.dto;
/** UTF-16 code units, matching Java String.length and browser maxlength. */
public final class TextLimits {
    private TextLimits() {}
    public static final int REVIEW = 50000;
    public static final int BIO = 5000;
    public static final int COMMENT = 5000;
    public static final int MESSAGE = 10000;
    public static final int NAME = 80;
}
