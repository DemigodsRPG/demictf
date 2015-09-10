package com.minegusta.demictf;

public enum CaptureTeam {
    RED('c'), BLUE('9');

    char code;
    String str;

    CaptureTeam(char code) {
        this.code = code;
        str = new String(new char[]{'§', code});
    }

    @Override
    public String toString() {
        return str;
    }
}
