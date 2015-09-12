package com.minegusta.demictf;

public enum CaptureTheFlagTeam {
    RED('c'), BLUE('9');

    char code;
    String str;

    CaptureTheFlagTeam(char code) {
        this.code = code;
        str = new String(new char[]{'§', code});
    }

    @Override
    public String toString() {
        return str;
    }

    public CaptureTheFlagTeam opposite() {
        if (this.equals(RED)) {
            return BLUE;
        }
        return RED;
    }
}
