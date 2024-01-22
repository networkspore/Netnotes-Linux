package com.utils;

public class Version implements Comparable<Version> {

    private String m_version;

    public final String get() {
        return m_version;
    }
    public Version(){
        m_version = "0.0.0";
    }

    public Version(String version) {
        if (version == null) {
            m_version = "0.0.0";
        }
        if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
            m_version = "0.0.0";
        }
        m_version = version;
    }

    @Override
    public int compareTo(Version that) {
        if (that == null) {
            return 1;
        }
        String[] thisParts = m_version.split("\\.");
        String[] thatParts = that.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length
                    ? Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length
                    ? Integer.parseInt(thatParts[i]) : 0;
            if (thisPart < thatPart) {
                return -1;
            }
            if (thisPart > thatPart) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }
        return this.compareTo((Version) that) == 0;
    }

    @Override
    public String toString(){

        return m_version != null ?( m_version.equals("0.0.0") ? "(Unknown)" : m_version) : "(Unknown)";
    }

}
