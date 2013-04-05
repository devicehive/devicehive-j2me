package com.dataart.devicehive;

import org.json.me.JSONArray;
import org.json.me.JSONObject;

/**
 * Extended JSON parser: supports single quotes and simple strings without quotes.
 */
public class ExJSONParser {
    private String buffer;
    private int offset;

    public ExJSONParser(String data) {
        this.buffer = data;
        this.offset = 0;
    }

    public Object parse() throws Exception {
        skipWS();

        int ch = peek();
        if (ch < 0) {
            throw new Exception("no JSON value");
        }

        // object
        if (ch == '{') {
            skip(1); // ignore '{'

            JSONObject jval = new JSONObject();
            boolean firstMember = true;

            while (true) {
                skipWS();

                ch = peek();
                if (ch == '}') {
                    skip(1); // ignore '}'
                    return jval; // end of object
                }

                if (!firstMember) { // check member separator
                    if (ch == ',') {
                        skip(1); // ingore ','
                        skipWS();
                    } else {
                        throw new Exception("no member separator");
                    }
                } else {
                    firstMember = false;
                }

                String memberName = parseString();

                skipWS();
                ch = peek();
                if (ch == ':') {
                    skip(1);
                } else {
                    throw new Exception("no member value separator");
                }

                jval.put(memberName, parse());
            }
        }

        // array
        else if (ch == '[') {
            skip(1); // ignore '['
            JSONArray jval = new JSONArray();
            boolean firstElement = true;

            while (true) {
                skipWS();

                ch = peek();
                if (ch == ']') {
                    skip(1); // ignore ']'
                    return jval; // end of array
                }

                if (!firstElement) {
                    if (ch == ',') {
                        skip(1); // ingore ','
                        skipWS();
                    } else {
                        throw new Exception("no element separator");
                    }
                }
                else {
                    firstElement = false;
                }

                jval.put(parse());
            }
        }

        // number: integer or double
        else if (Character.isDigit((char)ch) || (ch=='+') || (ch=='-')) {
            String s_num = getNum();

            try {
                return Integer.valueOf(s_num);
            } catch (Exception ex) {
                try {
                    return new Long(Long.parseLong(s_num));
                } catch (Exception ex2) {
                    try {
                        return Double.valueOf(s_num);
                    }  catch (Exception ex3) {
                        throw new Exception("cannot parse number");
                    }
                }
            }
        }

        // double-quoted or single-quoted string
        else if ((ch=='\"') || (ch=='\'')) {
            return parseQuotedString();
        }

        else if ((ch=='t') && match("true")) {
            return new Boolean(true);
        }

        else if ((ch=='f') && match("false")) {
            return new Boolean(false);
        }

        else if ((ch=='n') && match("null")) {
            return null;
        }

        // extension: simple strings [0-9A-Za-z_] without quotes
        else if (true) {
            return parseSimpleString();
        }

        /*
        else {
            throw new Exception("no valid JSON value");
        }
        */

        return null;
    }


    private int peek() {
        if (offset < buffer.length()) {
            return buffer.charAt(offset);
        }

        return -1; // end of stream
    }

    private int get() {
        int ch = peek();
        offset += 1;
        return ch;
    }

    private void skip(int count) {
        offset += count;
    }

    private String getNum() {
        StringBuffer sb = new StringBuffer();

        int ch = peek();
        if (Character.isDigit((char)ch) || (ch=='+') || (ch=='-')) {
            sb.append((char)ch);
            skip(1);
        }

        // integer part
        while (true) {
            ch = peek();
            if (Character.isDigit((char)ch)) {
                sb.append((char)ch);
                skip(1);
            } else {
                break;
            }
        }

        // floating point
        ch = peek();
        if (ch == '.') {
            sb.append((char)ch);
            skip(1);
        }

        // floating part
        while (true) {
            ch = peek();
            if (Character.isDigit((char)ch)) {
                sb.append((char)ch);
                skip(1);
            } else {
                break;
            }
        }

        // exponent
        ch = peek();
        if (ch == 'e' || ch == 'E') {
            sb.append((char)ch);
            skip(1);

            // exponent sign
            ch = peek();
            if (ch == '+' || ch == '-') {
                sb.append((char)ch);
                skip(1);
            }

            // exponent digits
            while (true) {
                ch = peek();
                if (Character.isDigit((char)ch)) {
                    sb.append((char)ch);
                    skip(1);
                } else {
                    break;
                }
            }
        }

        return sb.toString();
    }


    /** Skip whitespaces.
    */
    private boolean skipWS() {
        while (true) {
            final int ch = peek();
            if (ch < 0) {
                return false; // end of data
            }
            if (ch < ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                skip(1);
            } else {
                return true;
            }
        }
    }


    /**
     * @brief Parse quoted or simple string.
     */
    private String parseString() throws Exception {
        final int quote = peek();

        switch (quote) {
            case '\"': case '\'':
                return parseQuotedString();
        }

        return parseSimpleString();
    }


    /**
     * Parse quoted string.
     */
    private String parseQuotedString() throws Exception {
        // remember the "quote" character
        final int quote = this.get();

        StringBuffer sb = new StringBuffer();
        while (true) {
            final int ch = this.get();
            if (ch < 0) {
                throw new Exception("no data to parse string");
            }

            if (ch == quote) {
                return sb.toString();
            } else if (ch == '\\') { // escape
                final int ech = this.get();
                if (ech < 0) {
                    throw new Exception("no data to parse string");
                }

                switch (ech) {
                    case '"':  sb.append('"');  break;
                    case '\'': sb.append('\''); break;
                    case '/':  sb.append('/');  break;
                    case '\\': sb.append('\\'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;

                    case 'u': {
                        final int a = Character.digit((char)this.get(), 16);
                        final int b = Character.digit((char)this.get(), 16);
                        final int c = Character.digit((char)this.get(), 16);
                        final int d = Character.digit((char)this.get(), 16);
                        final int code = (a<<12) | (b<<8) | (c<<4) | d;
                        sb.append((char)code);
                    } break;

                    default:
                        throw new Exception("bad escape sequence in a string");
                }
            } else {
                sb.append((char)ch); // just save
            }
        }
    }


    /**
     * Parse simple string.
     */
    private String parseSimpleString() throws Exception {
        StringBuffer sb = new StringBuffer();
        while (true) {
            final int ch = this.peek();
            if (('0' <= ch && ch <= '9')
                || ('a' <= ch && ch <= 'z')
                || ('A' <= ch && ch <= 'Z')
                || '_' == ch) {
                sb.append((char)ch);
                skip(1);
            } else {
                if (sb.length() == 0) {
                    throw new Exception("no data to parse string");
                } else {
                    return sb.toString();
                }
            }
        }
    }


    /**
     * Match an input with a pattern.
     */
    private boolean match(String pattern) {
        return buffer.indexOf(pattern, offset) == offset;
    }
}
