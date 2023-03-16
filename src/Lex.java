import java.util.*;

/**
 * 词法分析程序
 *
 * @author 沈慧昌
 * @date 2022年12月20日12:03:46
 */
public class Lex {
    //chart保存源程序中分割好的单词序列
    List<String> chart = new ArrayList<>();
    //使用Map保存词法分析的结果 key为单词，value为这个单词的类别
    public Map<String, String> Lex = new HashMap<>();
    //当前指针位置
    private int textAt = 0;
    //当前行位置
    private int rowCount = 1;
    //源程序代码
    private String text;
    //关键字数组
    private final String[] keys = {
            "main", "void", "bool", "int", "double", "char", "float", "printf",
            "class", "scanf", "else", "if", "return", "char", "public", "static"
            , "true", "false", "private", "while", "auto", "new",
            "continue", "break"
    };


    public Lex(String text) {
        this.text = text;
    }


    /**
     * 判断字符是否为字母
     *
     * @param ch 输入字符
     * @return 是字母为true
     */
    public boolean isCharacter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    /**
     * 判断字符是否为数字
     *
     * @param ch 输入字符
     * @return 是数字为true
     */
    public boolean isNum(char ch) {
        return (ch >= '0' && ch <= '9');
    }

    /**
     * 判断字符是否为关键字
     *
     * @param string 输入字符串
     * @return 是关键字为true
     */
    public boolean isKey(String string) {
        for (String key : keys) {
            if (string.equals(key))
                return true;
        }
        return false;
    }

    /**
     * 开始对输入代码串进行分析
     */
    public void Scanner() {
        char ch;
        //为源程序加上结束符'\0'
        text = text + '\0';
        //遍历源程序
        while (textAt < text.length() - 1) {
            ch = text.charAt(textAt);
            //如果遇到空格符或者制表符，直接跳过
            if (ch == ' ' || ch == '\t')
                textAt++;
            //如果遇到换行符，则行数+1
            else if (ch == '\r' || ch == '\n') {
                textAt++;
                rowCount++;
            } else {
                //其余情况对当前字符进行词法分析
                textAt = ScannerAt(textAt);
            }
        }
    }

    /**
     * 根据单词的首字符进行词法分析
     */
    public int ScannerAt(int textAt) {
        //获取到当前处理字符ch
        int i = textAt;
        char ch = text.charAt(i);
        String string;
        if (isCharacter(ch)) {
            //如果ch是字母的话，那么这个单词的可能类别是标识符或者关键字，进入首字符是字母的子程序中进行进一步处理！
            string = "" + ch;
            return handleFirstCharacter(i, string);
        } else if (isNum(ch)) {
            //如果ch是数字的话，那么这个单词的类别是常数，可能是整数也可能是小数。
            string = "" + ch;
            return handleFirstNum(i, string);
        } else {
            //其他字符的话，就是界符或者运算符
            string = "" + ch;
            switch (ch) {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    return ++i;
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case ',':
                case '.':
                case ';':
                    printResult("界符", string);
                    return ++i;
                case '=':
                    if (text.charAt(i + 1) == '=') {
                        printResult("关系运算符", string + text.charAt(i + 1));
                        return i + 2;
                    } else {
                        printResult("赋值运算符", string);
                        return ++i;
                    }


                case '+':
                    return handlePlus(i, string);
                case '-':
                    return handleMinus(i, string);
                case '*':
                    return handleMulti(i, string);
                case '/':
                    if (text.charAt(i + 1) == '/') {
                        return handleSingleLineNote(i, string);
                    } else if (text.charAt(i + 1) == '*') {
                        return handleNote(i, string);
                    } else if (text.charAt(i + 1) == '=') {
                        return handleDiv(i, string);
                    } else
                        return handleDiv(i, string);
                case '<':
                case '>':
                case '!':
                    if (text.charAt(i + 1) == '=') {
                        printResult("关系运算符", string + text.charAt(i + 1));
                        return i + 2;
                    } else {
                        printResult("关系运算符", string);
                        return ++i;
                    }
                case '&':
                    printResult("关系运算符", string + text.charAt(i + 1));
                    return i + 2;
                case '|':
                    if (text.charAt(i + 1) == '|') {
                        printResult("关系运算符", string + text.charAt(i + 1));
                        return i + 2;
                    }


                case '\\':
                    if (text.charAt(i + 1) == 'n' || text.charAt(i + 1) == 't' ||
                            text.charAt(i + 1) == 'r' || text.charAt(i + 1) == '\\' ||
                            text.charAt(i + 1) == 'a' || text.charAt(i + 1) == 'v' ||
                            text.charAt(i + 1) == 'b' || text.charAt(i + 1) == 'f' ||
                            text.charAt(i + 1) == '\'' || text.charAt(i + 1) == '\"') {
                        printResult("转义符", string + text.charAt(i + 1));
                        return i + 2;
                    }
                case '\'':
                    return handleChar(i, string);
                case '\"':
                    return handleString(i, string);
                default:
                    printError("错误：暂时无法识别的标识符", string);
                    return ++i;
            }
        }
    }

    /**
     * 处理多行注释
     *
     * @param charAt 指针位置
     * @param string 首字符
     * @return 处理结束时指针位置
     */
    private int handleNote(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        StringBuilder st = new StringBuilder(string + ch);
        ch = text.charAt(++i);
        while (text.charAt(i) != '*' || (i + 1) < text.length() && text.charAt(i + 1) != '/') {
            st.append(ch);
            if (ch == '\n' || ch == '\r')
                rowCount++;
            else if (ch == '\0') {
                printError("错误：注释未闭合", st.toString());
                return i;
            }
            ch = text.charAt(++i);
        }
        st.append("*/");
        printResult("多行注释符", st.toString());
        return i + 2;
    }

    /**
     * 处理单行注释
     *
     * @param charAt 指针位置
     * @param string 首字符
     * @return 处理结束时指针位置
     */
    private int handleSingleLineNote(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        StringBuilder st = new StringBuilder(string + ch);
        ch = text.charAt(++i);
        while (text.charAt(i) != '\n' && text.charAt(i) == '\r') {
            st.append(ch);
            ch = text.charAt(++i);
        }
        printResult("单行注释符", st.toString());
        return ++i;
    }

    private int handleDiv(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        String st = string;
        if (ch == '=') {
            st = st + ch;
            printResult("除法运算符", st);
            return ++i;

        } else {
            printResult("除法运算符", st);
            return i;
        }
    }

    private int handleMulti(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        String st = string;
        if (ch == '=') {
            st = st + ch;
            printResult("乘法运算符", st);
            return ++i;

        } else {
            printResult("乘法运算符", st);
            return i;
        }
    }

    private int handleMinus(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        String st = string;
        if (ch == '-') {
            st = st + ch;
            printResult("减法运算符", st);
            return ++i;
        } else if (ch == '=') {
            st = st + ch;
            printResult("减法运算符", st);
            return ++i;
        } else {
            printResult("减法运算符", st);
            return i;
        }
    }

    private int handlePlus(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        String st = string;
        if (ch == '+') {
            st = st + ch;
            printResult("加法运算符", st);
            return ++i;
        } else if (ch == '=') {
            st = st + ch;
            printResult("加法运算符", st);
            return ++i;
        } else {
            printResult("加法运算符", st);
            return i;
        }
    }


    private int handleChar(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        StringBuilder st = new StringBuilder(string);
        while (ch != '\'') {
            if (ch == '\n' || ch == '\r')
                rowCount++;
            else if (ch == '\0') {
                printError("错误：单字符没有闭合", st.toString());
                return i;
            }
            st.append(ch);
            ch = text.charAt(++i);
        }
        st.append(ch);
        if (st.length() == 3 || st.toString().equals("\\'" + "\\" + "t" + "\\") || st.toString().equals("\\'" + "\\" + "n" + "\\") || st.toString().equals("\\'" + "\\" + "r" + "\\"))
            printResult("单字符", st.toString());
        else
            printError("单字符溢出", st.toString());
        return ++i;
    }

    private int handleString(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        StringBuilder st = new StringBuilder(string);
        while (ch != '\"') {
            if (ch == '\n' || ch == '\r')
                rowCount++;
            else if (ch == '\0') {
                printError("错误：字符串未闭合", st.toString());
                return i;
            }
            st.append(ch);
            ch = text.charAt(++i);
        }
        st.append(ch);
        printResult("字符串", st.toString());
        return ++i;
    }

    /**
     * 处理首字符是字母的情况
     *
     * @param charAt 当前指针位置
     * @param string 单词的首字符
     * @return
     */
    public int handleFirstCharacter(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        StringBuilder st = new StringBuilder(string);
        //根据标识符只含字母、数字和下划线的原则，将这个单词提取出来；然后再判断是否属于关键字，如果不属于，则为普通标识符。
        while (isCharacter(ch) || isNum(ch) || ch == '_') {
            st.append(ch);
            ch = text.charAt(++i);
        }
        if (st.length() == 1) {
            printResult("普通标识符", st.toString());
            return i;
        }
        if (isKey(st.toString())) {
            printResult("关键字", st.toString());
        } else {
            printResult("普通标识符", st.toString());
        }
        return i;
    }

    /**
     * 处理首字符是数字的情况
     *
     * @param charAt 当前指针位置
     * @param string 单词的首字符
     * @return
     */
    public int handleFirstNum(int charAt, String string) {
        int i = charAt;
        char ch = text.charAt(++i);
        StringBuilder st = new StringBuilder(string);
        while (isNum(ch)) {
            st.append(ch);
            ch = text.charAt(++i);
        }

        if (ch == ' ' || ch == ';' || ch == ',' || ch == '\n' || ch == '\r' || ch == '\0' ||
                ch == '\t' || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%' ||
                ch == ')' || ch == ']') {
            printResult("整数", st.toString());
            return i;
        } else if (ch == '.' && isNum(text.charAt(i + 1))) {
            st.append(ch);
            ch = text.charAt(++i);
            while (isNum(ch)) {
                st.append(ch);
                ch = text.charAt(++i);
            }
            if (ch == ' ' || ch == ';' || ch == ',' || ch == '\n' || ch == '\r' || ch == '\0' ||
                    ch == '\t' || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%' ||
                    ch == ')' || ch == ']') {
                printResult("浮点数", st.toString());
            } else {
                do {
                    st.append(ch);
                    ch = text.charAt(++i);
                } while (ch != ' ' && ch != ';' && ch != ',' && ch != '\n' && ch != '\r' && ch != '\0' &&
                        ch != '\t' && ch != '+' && ch != '-' && ch != '*' && ch != '/' && ch != '%' &&
                        ch != ')' && ch != ']');
                printError("错误：输入不合法", st.toString());
            }
            return i;
        } else {
            do {
                st.append(ch);
                ch = text.charAt(++i);
            } while (ch != ' ' && ch != ';' && ch != ',' && ch != '\n' && ch != '\r' && ch != '\0' &&
                    ch != '\t' && ch != '+' && ch != '-' && ch != '*' && ch != '/' && ch != '%' &&
                    ch != ')' && ch != ']');
            printError("错误：输入不合法", st.toString());
            return i;
        }
    }

    /**
     * 输出词法分析结果并保存到Lex中
     *
     * @param token 单词类别
     * @param string 单词
     */
    public void printResult(String token, String string) {
        System.out.println(token + "：" + string);
        Lex.put(string, "<" + token + ">");
        chart.add(string);
    }

    /**
     * 输出错误信息，定位到程序的行和错误的单词
     *
     * @param error 错误类型
     * @param string 错误单词
     */
    public void printError(String error, String string) {
        System.out.println("第" + rowCount + "行" + error + "：" + string);
        System.exit(11111);
    }

}
