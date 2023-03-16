import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 语法制导的翻译，中间代码为三地址码TAC
 *      在这个类中完成语法分析的同时将源程序转换为中间代码
 *      因为采用SLR(1)语法分析方法，所以本质上就是根据SLR(1)分析表不断地进行移入、归约操作
 *
 * @author 沈慧昌
 * @date 2022年12月22日07:21:15
 */
public class Parsing {
    //状态栈
    Stack<String> statusStack;
    //符号栈
    Stack<String> tokenStack;
    //输入流
    List<String> inputString;
    //记录语法分析过程中执行的所有操作，包括移入和归约
    List<String> ACTION;
    //记录执行归约操作后查找SLR分析表的GOTO表得到的状态
    List<String> GOTO;
    //三地址码集合
    List<TAC> tacList;
    //AList和RList用于生成三地址码
    List<String> AList;
    List<Integer> RList;
    //构造好的有限自动机
    Analysis analysis;
    //SLR(1)分析表
    SLRChart SLRChart;
    //三地址码的数量
    int TACcount = 0;
    //词法分析结果
    Lex lex;

    public Parsing(Analysis analysis, Lex lex, SLRChart SLRChart) {
        this.analysis = analysis;
        this.lex = lex;
        this.SLRChart = SLRChart;
        tokenStack = new Stack<>();
        statusStack = new Stack<>();
        inputString = new ArrayList<>();
        ACTION = new ArrayList<>();
        GOTO = new ArrayList<>();
        tacList = new ArrayList<>();
        RList = new ArrayList<>();
        AList = new ArrayList<>();

        //初始化，向状态栈中加入I0，符号栈中加入#
        tokenStack.push("#");
        statusStack.push("I0");

    }

    private void print(int count, String ACTIONstring, String GOTOstring) {
        String statusprintString = "";
        String tokenprintString = "";
        String inputprintString = "";
        for (String s :
                tokenStack) {
            tokenprintString += s;
        }
        for (String s :
                statusStack) {
            statusprintString += s;
        }
        for (String s :
                inputString) {
            inputprintString += s;
        }
        System.out.println(count + "   "
                + statusprintString + "   "
                + tokenprintString + "   "
                + inputprintString + "   "
                + ACTIONstring + "   "
                + GOTOstring);
    }


    public int parsing() {
        int cnt = 0;
        //输入流就是词法分析结果再加上结束符号'#'
        inputString.addAll(lex.chart);
        inputString.add("#");

        //进行语法分析，并且进行语法制导的翻译过程
        while (true) {
            //当前输入单词
            String tempInput = inputString.get(0);
            //当前状态栈栈顶状态
            String status = statusStack.peek();
            //查SLR(1)分析表
            String action;
            if (analysis.terminals.contains(tempInput)) {
                action = SLRChart.ACTION.get(status).get(tempInput);
            } else {
                action = SLRChart.ACTION.get(status).get(lex.Lex.get(tempInput));
            }

            try {
                //如果要进行的操作包含"S"，那么显然是移入操作
                if (action.contains("S")) {
                    print(++cnt, action, "");
                    ACTION.add(action);
                    //那么将对应的状态移入状态栈
                    statusStack.push(action.replace("S", "I"));
                    //以及当前输入符号移入符号栈
                    tokenStack.push(tempInput);
                    //输入流指向下一个单词
                    inputString.remove(0);
                } else if (action.contains("r")) {
                    //如果要进行的操作包含"r"，那么显然是进行归约操作
                    //那么我们首先要找到用于归约的那个产生式，然后根据选用的不同归约产生式，执行不同的语义规则，形成三地址码
                    ACTION.add(action);

                    //tokenOut记录归约串
                    String tokenOut = "";
                    //GOTOnum就是进行归约的产生式的序号
                    int GOTOnum = Integer.parseInt(action.replace("r", ""));

//                    System.out.println("进行归约的产生式：GOTOnum = " + GOTOnum);

                    //将这个归约产生式的右部出符号栈，并且将对应数量的状态退出状态栈
                    for (int i = 0; i < analysis.productions.get(GOTOnum).getRight().length; i++) {
                        statusStack.pop();
                        String tmp = tokenStack.pop();
                        if (analysis.terminals.contains(tmp))
                            tokenOut += tmp;
                        else if (analysis.terminals.contains(lex.Lex.get(tmp)))
                            tokenOut += tmp;
                    }

                    //然后将这个归约产生式左部的非终结符入符号栈
                    String nonterminal = analysis.productions.get(GOTOnum).getLeft();
                    tokenStack.push(nonterminal);

//                    System.out.println("产生式左部非终结符为" + nonterminal);

                    //然后查GOTO表，将 当前状态栈栈顶状态 遇到 当前归约产生式左部的非终结符 作为输入符 后得到的状态入栈
                    String gotoString = SLRChart.GOTO.get(statusStack.peek()).get(nonterminal);
                    statusStack.push(gotoString);

//                    System.out.println("对应执行入栈的状态为" + gotoString);

                    GOTO.add(gotoString);

                    //输出语法分析的结果！
                    print(++cnt, action, GOTO.get(GOTO.size() - 1));

                    //然后根据进行归约的产生式的序号来进行语义分析
                    //不同的产生式对应不同的语义规则
                    //产生式序号由0开始，0 ~ 26
                    switch (GOTOnum) {
                        //归约产生式为：(2) S -> PS
                        case 2:
                            emit2();
                            break;
                        //归约产生式为：(6) C -> if ( boolean )
                        case 6:
                            emit6();
                            break;
                        //归约产生式为：(7) P -> C M else
                        case 7:
                            emit7();
                            break;
                        //归约产生式为：(12) L -> L L
                        case 12:
                            emit12();
                            break;
                        //归约产生式为：(13) boolean -> E <关系运算符> E
                        case 13:
                            emit13(tokenOut);
                            break;
                        //归约产生式为：(16) A -> <普通标识符> = E
                        case 16:
                            emit16(tokenOut);
                            break;
                        //归约产生式为：(17) E -> E + T
                        case 17:
                            emit17();
                            break;
                        //归约产生式为：(19) E -> E - T
                        case 19:
                            emit19();
                            break;
                        //归约产生式为：(20) T -> T * F
                        case 20:
                            emit20();
                            break;
                        //归约产生式为：(22) T -> T / F
                        case 22:
                            emit22();
                            break;
                        //归约产生式为：(23) F -> <普通标识符>      (24) F -> <整数>       (25) F -> <浮点数>
                        case 23:
                        case 24:
                        case 25:
                            emit23(tokenOut);
                            break;
                    }

                } else if (action.equals("acc")) {
                    print(++cnt, "acc", "");
                    tacList.add(new TAC("out"));
                    printTAC();
                    return 1;
                }

            } catch (Exception e) {
                return 0;
            }
        }
    }

    private void printTAC() {
        System.out.println("\n----------------输出三地址码--------------");
        for (int i = 0; i < tacList.size(); i++) {
            TAC tac = tacList.get(i);
            System.out.println("(" + (i + 1) + ") " + tac.getExpr());
        }

    }


    /**
     * 产生式2进行归约
     * (2) S -> PS      对goto后面的占位符进行回填
     */
    private void emit2() {
        for (String string : AList) {
            TAC tac = new TAC(string);
            tacList.add(tac);
        }
        for (int i = tacList.size() - 1; i >= 0; i--) {
            String expr = tacList.get(i).getExpr();
            //回填goto后面的占位符
            if (expr.contains("goto") && expr.contains("--")) {
                tacList.get(i).setExpr(expr.replace("--", "(" + (tacList.size() + 1) + ")"));
                break;
            }
        }
        AList.clear();
        RList.clear();
    }



    private void emit6() {
        //(6) C -> if ( boolean )
        //三地址码就是两条
        //1、条件为真goto __
        //2、条件为假goto __
        TAC tac = new TAC("if " + AList.remove(AList.size() - 1) + " goto (" + (tacList.size() + 3) + ")");
        tacList.add(tac);
        tacList.add(new TAC("goto --"));
    }

    private void emit7() {
        //(7) P -> C M else
        for (String string :
                AList) {
            TAC tac = new TAC(string);
            tacList.add(tac);
        }

        //因为出现了else，也就是之前if的假出口，所以需要回填goto后面的占位符
        for (int i = tacList.size() - 1; i >= 0; i--) {
            String expr = tacList.get(i).getExpr();
            if (expr.contains("goto") && expr.contains("--")) {
                tacList.get(i).setExpr(expr.replace("--", "(" + (tacList.size() + 2) + ")"));
                break;
            }
        }
        //但是当前else何处结束还未知
        tacList.add(new TAC("goto --"));
        AList.clear();
        RList.clear();
    }


    private void emit12() {
        //(12) L -> L L
        int e1 = RList.get(RList.size() - 1);
        int e2 = RList.get(RList.size() - 2);
        RList.remove(e1);
        RList.remove(e2);
        RList.add(e1 + e2);
    }

    private void emit13(String op) {//op为关系运算符
        //(13) boolean -> E <关系运算符> E
        //取出进行关系运算的两个普通标识符e1和e2
        //然后将e1 op e2作为整体加入到AList中
        String e1 = AList.get(AList.size() - 2);
        String e2 = AList.get(AList.size() - 1);
        AList.remove(e1);
        AList.remove(e2);
        AList.add(e1 + " " + op + " " + e2);
    }


    private void emit16(String token) {//token是普通标识符
        //(16) A -> <普通标识符> = E
        String[] tokens = token.split("");

        String e2 = AList.get(AList.size() - 1);

        AList.remove(e2);
        //赋值语句的三地址码
        tacList.add(new TAC(tokens[tokens.length - 1] + " = " + e2));
    }

    //emit17()、emit19()、emit20()、emit22()操作类似，只需要替换运算符即可
    /**
     * 产生式17：E -> E + T进行归约
     *      需要生成三地址码
     */
    private void emit17() {
        String e1 = AList.get(AList.size() - 2);
        String e2 = AList.get(AList.size() - 1);
        AList.remove(e1);
        AList.remove(e2);
        //零时变量Ti
        tacList.add(new TAC("T" + (++TACcount) + " = " + e1 + " + " + e2));
        AList.add("T" + TACcount);
    }

    /**
     * 产生式19：E -> E - T进行归约
     *      需要生成三地址码
     */
    private void emit19() {
        String e1 = AList.get(AList.size() - 2);
        String e2 = AList.get(AList.size() - 1);
        AList.remove(e1);
        AList.remove(e2);
        tacList.add(new TAC("T" + (++TACcount) + " = " + e1 + " - " + e2));
        AList.add("T" + TACcount);
    }

    private void emit20() {
        String e1 = AList.get(AList.size() - 2);
        String e2 = AList.get(AList.size() - 1);
        AList.remove(e1);
        AList.remove(e2);
        tacList.add(new TAC("T" + (++TACcount) + " = " + e1 + " * " + e2));
        AList.add("T" + TACcount);
    }

    private void emit22() {
        String e1 = AList.get(AList.size() - 2);
        String e2 = AList.get(AList.size() - 1);
        AList.remove(e1);
        AList.remove(e2);
        tacList.add(new TAC("T" + (++TACcount) + " = " + e1 + " / " + e2));
        AList.add("T" + TACcount);
    }

    /**
     * (23) F -> <普通标识符>
     * (24) F -> <整数>
     * (25) F -> <浮点数>
     *      只需要将归约串添加到AList中即可
     *
     * @param e 归约串
     */
    private void emit23(String e) {
        AList.add(e);
    }


}
