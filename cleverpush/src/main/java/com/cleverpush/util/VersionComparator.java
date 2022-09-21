package com.cleverpush.util;

import com.cleverpush.banner.models.VersionComparison;

public class VersionComparator {

    public boolean equals(Object o1, Object o2) {
        return compare(o1, o2) == VersionComparison.EQUALS;
    }

    public VersionComparison compare(Object o1, Object o2) {
        String version1 = (String) o1;
        String version2 = (String) o2;

        VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
        VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

        int number1, number2;
        String suffix1, suffix2;

        while (tokenizer1.MoveNext()) {
            if (!tokenizer2.MoveNext()) {
                do {
                    number1 = tokenizer1.getNumber();
                    suffix1 = tokenizer1.getSuffix();
                    if (number1 != 0 || suffix1.length() != 0) {
                        // Version one is longer than number two, and non-zero
                        return VersionComparison.GREATER_THAN;
                    }
                }
                while (tokenizer1.MoveNext());

                // Version one is longer than version two, but zero
                return VersionComparison.EQUALS;
            }

            number1 = tokenizer1.getNumber();
            suffix1 = tokenizer1.getSuffix();
            number2 = tokenizer2.getNumber();
            suffix2 = tokenizer2.getSuffix();

            if (number1 < number2) {
                // Number one is less than number two
                return VersionComparison.LESS_THAN;
            }
            if (number1 > number2) {
                // Number one is greater than number two
                return VersionComparison.GREATER_THAN;
            }

            boolean empty1 = suffix1.length() == 0;
            boolean empty2 = suffix2.length() == 0;

            if (empty1 && empty2) {
                continue; // No suffixes
            }

            if (empty1) {
                return VersionComparison.GREATER_THAN; // First suffix is empty (1.2 > 1.2b)
            }

            if (empty2) {
                return VersionComparison.LESS_THAN; // Second suffix is empty (1.2a < 1.2)
            }

            // Lexical comparison of suffixes
            int result = suffix1.compareTo(suffix2);
            if (result != 0) {
                return VersionComparison.valueOf(String.valueOf(result));
            }

        }
        if (tokenizer2.MoveNext()) {
            do {
                number2 = tokenizer2.getNumber();
                suffix2 = tokenizer2.getSuffix();
                if (number2 != 0 || suffix2.length() != 0) {
                    // Version one is longer than version two, and non-zero
                    return VersionComparison.LESS_THAN;
                }
            }
            while (tokenizer2.MoveNext());

            // Version two is longer than version one, but zero
            return VersionComparison.EQUALS;
        }
        return VersionComparison.EQUALS;
    }

    // VersionTokenizer.java
    public static class VersionTokenizer {
        private final String _versionString;
        private final int _length;

        private int _position;
        private int _number;
        private String _suffix;
        private boolean _hasValue;

        VersionTokenizer(String versionString) {
            if (versionString == null) {
                throw new IllegalArgumentException("versionString is null");
            }
            _versionString = versionString;
            _length = versionString.length();
        }

        public int getNumber() {
            return _number;
        }

        String getSuffix() {
            return _suffix;
        }

        public boolean hasValue() {
            return _hasValue;
        }

        boolean MoveNext() {
            _number = 0;
            _suffix = "";
            _hasValue = false;

            // No more characters
            if (_position >= _length) {
                return false;
            }

            _hasValue = true;

            while (_position < _length) {
                char c = _versionString.charAt(_position);
                if (c < '0' || c > '9') {
                    break;
                }
                _number = _number * 10 + (c - '0');
                _position++;
            }

            int suffixStart = _position;

            while (_position < _length) {
                char c = _versionString.charAt(_position);
                if (c == '.') {
                    break;
                }
                _position++;
            }

            _suffix = _versionString.substring(suffixStart, _position);

            if (_position < _length) {
                _position++;
            }

            return true;
        }
    }
}
