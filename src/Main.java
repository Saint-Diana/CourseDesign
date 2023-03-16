import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 主程序
 *
 * @author 沈慧昌
 * @date 2022年12月20日12:04:012
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("**********编译原理课程设计程序**********" + "\n源程序代码为：");
        //读取源文件"Code"，获取到源程序，将其中内容作为字符串text处理
        StringBuilder text = new StringBuilder();
        //line是指当前处理的那一行代码
        String line;
        InputStream is = Files.newInputStream(Paths.get("Code"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        //每次从源程序中读取一行代码，拼接到text上
        line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            text.append(line);
            line = reader.readLine();
        }
        reader.close();
        is.close();

        //进行词法分析，输入是源程序代码text
        Lex lex = new Lex(text.toString());
        lex.Scanner();
//        System.out.println("************************");
//        System.out.println("输出一下lex.Lex");
//        System.out.println(lex.Lex);
//        System.out.println("lex.chart = " + lex.chart);
//        System.out.println("************************");
        //根据文法构造识别活前缀的DFA
        Analysis analysis = new Analysis(lex);
//        System.out.println("************************");
//        System.out.println("输出一下analysis中的数据结构");
//        System.out.println("analysis.productions = " + analysis.productions);
//        System.out.println("analysis.itemSet = " + analysis.itemSet);
//        System.out.println("analysis.DFA = " + analysis.DFA);
//        System.out.println("analysis.terminals = " + analysis.terminals);
//        System.out.println("analysis.nonterminals = " + analysis.nonterminals);
//        System.out.println("analysis.endList = " + analysis.endList);
//        System.out.println("************************");
        //然后根据构造好的项目集族，构造SLR(1)分析表
        SLRChart SLRChart = new SLRChart(analysis);
//        System.out.println("************************");
//        System.out.println("输出一下SLR分析表中数据结构");
//        System.out.println("action = " + SLRChart.ACTION);
//        System.out.println("goto = " + SLRChart.GOTO);
//        System.out.println("************************");
        Parsing parsing = new Parsing(analysis, lex, SLRChart);
        System.out.println("步骤\t\t\t\t  状态栈\t\t\t\t符号栈\t\t\t\t  输入流\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t操作\t\t\t\t\t\tGOTO");
        if (parsing.parsing() == 0) {
            System.out.println("出错");
        }
//        System.out.println("************************");
//        System.out.println("输出一下Parsing中数据结构");
//        System.out.println("inputString = " + parsing.inputString);
//        System.out.println("ACTION = " + parsing.ACTION);
//        System.out.println("GOTO = " + parsing.GOTO);
//        System.out.println("statusStack = " + parsing.statusStack);
//        System.out.println("tokenStack = " + parsing.tokenStack);
//        System.out.println("AList = " + parsing.AList);
//        System.out.println("RList = " + parsing.RList);
//        System.out.println("tacList = " + parsing.tacList);
//        System.out.println("************************");
    }
}
