package io.github.foadsf.kangaroohops;

/** Numbers as she sees them: Persian digits (۰۱۲) in Farsi, Western digits otherwise. Plain Java. */
final class Numbers {
    static final String[] LANGS = {"fa", "nl", "en"};
    static final String[] LABEL = {"فا", "NL", "EN"};

    /** The number in the digits of {@code lang}. Persian digits are U+06F0..U+06F9. */
    static String digits(String lang, int n) {
        String s = Integer.toString(n);
        if (!"fa".equals(lang)) return s;
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) sb.append(ch == '-' ? '-' : (char) ('۰' + (ch - '0')));
        return sb.toString();
    }

    /** Words for 0-20, for a speech engine and as the spoken fallback text. */
    static String word(String lang, int n) {
        String[] w;
        switch (lang) {
            case "fa":
                w = new String[]{"صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
                        "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده", "بیست"};
                break;
            case "nl":
                w = new String[]{"nul", "een", "twee", "drie", "vier", "vijf", "zes", "zeven", "acht", "negen",
                        "tien", "elf", "twaalf", "dertien", "veertien", "vijftien", "zestien", "zeventien",
                        "achttien", "negentien", "twintig"};
                break;
            default:
                w = new String[]{"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
                        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
                        "eighteen", "nineteen", "twenty"};
        }
        return n >= 0 && n < w.length ? w[n] : Integer.toString(n);
    }

    private Numbers() {
    }
}
