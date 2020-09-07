package com.lez.setting;

/**
 * 按位获取工具类
 */
public class SettingSupport {
    private SettingSupport() {
    }

    /**
     * 获取v的从fromIndex到toIndex的数
     */
    public static long getPosValue(final long v, final int fromIndex, final int toIndex) {
        if (fromIndex < 0 || toIndex > Long.SIZE || fromIndex >= toIndex) {
            throw new RuntimeException("错误索引 fromIndex:" + fromIndex + " toIndex:" + toIndex);
        }

        int indexNum = toIndex - fromIndex;
        long fullOne = full[indexNum - 1];

        long fullOneShifted = fullOne << fromIndex;
        return (v & fullOneShifted) >>> fromIndex;
    }

    /**
     * 设置oldv从fromIndex到toIndex的值为setV
     */
    public static long setPosValue(final long oldV, final int fromIndex, final int toIndex, final long setV) {
        if (fromIndex < 0 || toIndex > Long.SIZE || fromIndex >= toIndex) {
            throw new RuntimeException("错误索引 fromIndex:" + fromIndex + " toIndex:" + toIndex);
        }

        int indexNum = toIndex - fromIndex;
        long fullOne = full[indexNum - 1];

        if (setV < 0) {
            throw new RuntimeException("设置值必须大于0  setV:" + setV);
        }
        if (indexNum != Long.SIZE && setV > fullOne) {
            throw new RuntimeException("设置值过大 最大设置值:" + fullOne + " oldV:" + oldV + "  fromIndex:" + fromIndex + " toIndex:" + toIndex + " setV:" + setV);
        }

        long fullOneReversed = ~(fullOne << fromIndex);

        long setVShifted = setV << fromIndex;

        long newV = oldV & fullOneReversed; //清空
        newV = newV | setVShifted; //设置新值

        return newV;
    }

    public static int getPosValue(final int v, final int fromIndex, final int toIndex) {
        if (fromIndex < 0 || toIndex > Integer.SIZE || fromIndex >= toIndex) {
            throw new RuntimeException("错误索引 fromIndex:" + fromIndex + " toIndex:" + toIndex);
        }
        return (int) getPosValue((long) v, fromIndex, toIndex);
    }

    public static int setPosValue(final int oldV, final int fromIndex, final int toIndex, final int setV) {
        if (fromIndex < 0 || toIndex > Integer.SIZE || fromIndex >= toIndex) {
            throw new RuntimeException("错误索引 fromIndex:" + fromIndex + " toIndex:" + toIndex);
        }
        return (int) setPosValue((long) oldV, fromIndex, toIndex, setV);
    }

    private static final long[] full;

    static {
        full = new long[Long.SIZE];
        for (int i = 0; i < Long.SIZE; i++) {
            if (i == 0) {
                full[i] = 1;
            } else {
                full[i] = (full[i - 1] << 1) + 1;
            }
        }


    }

    public static void main(String[] args) {
        int v = -1;
        printNumber(v);

        int from = 0;
        int to = 32;
        System.out.println(getPosValue(v, from, to));

        v = setPosValue(v, from, to, 0);
        printNumber(v);
        System.out.println(getPosValue(v, from, to));

        v = setPosValue(v, from, to, 7);
        printNumber(v);
        System.out.println(getPosValue(v, from, to));

    }

    private static void printNumber(int v) {
        String s = Long.toBinaryString(v);
        for (int i = s.length(); i < Integer.SIZE; i++) {
            System.out.print('0');
        }
        System.out.println(s);
    }

    private static void printNumber(long v) {
        String s = Long.toBinaryString(v);
        for (int i = s.length(); i < Long.SIZE; i++) {
            System.out.print('0');
        }
        System.out.println(s);
    }
}
