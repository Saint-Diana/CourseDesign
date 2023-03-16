import consoletable.ConsoleTable;
import consoletable.table.Cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * SLR1分析法，构造SLR1分析表
 *      SLR(1)分析法需要用到FOLLOW集来处理移入、归约/归约、归约冲突
 *      根据DFA来构造SLR(1)分析表，主要是要注意既存在移入项目又存在归约项目的项目集
 *
 * @author 沈慧昌
 * @date 2022年12月25日15:10:16
 */
public class SLRChart {
    //各个文法符号的first集
    private HashMap<String, List<String>> firsts;
    //各个文法符号的follow集
    private HashMap<String, List<String>> follows;
    //移入项目集合
    private List<Production> MoveinList;
    //归约项目集合
    private List<Production> GuiyueList;
    private List<String> CanBeNull;
    //构造好的有限自动机
    Analysis analysis;
    //ACTION表
    HashMap<String, HashMap<String, String>> ACTION;
    //GOTO表
    HashMap<String, HashMap<String, String>> GOTO;

    public SLRChart(Analysis analysis) {
        this.analysis = analysis;
        firsts = new HashMap<>();
        follows = new HashMap<>();
        CanBeNull = new ArrayList<>();
        ACTION = new HashMap<>();
        GOTO = new HashMap<>();

        //首先求每个文法符号的first集和follow集
        setFirsts();
        setFollows();
        //然后根据DFA和follow集得到SLR(1)分析表，分为ACTION表和GOTO表
        setGOTO();
        setACTION();

        printChart();
    }

    /**
     * 打印SLR分析表，利用IDEA自动生成的consoletable
     */
    private void printChart() {
        List<Cell> header = new ArrayList<>();
        header.add(new Cell("编号"));
        for (String String :
                analysis.terminals) {
            header.add(new Cell(String));
        }

        for (String String :
                analysis.nonterminals) {
            header.add(new Cell(String));
        }

        List<List<Cell>> body = new ArrayList<>();
        List<Cell> bodyCell;
        for (int i = 0; i < ACTION.size(); i++) {
            bodyCell = new ArrayList<>();
            bodyCell.add(new Cell("I" + i));

            for (int j = 1; j <= analysis.terminals.size(); j++) {
                Cell cell = header.get(j);
                int cnt = 0;
                for (String string :
                        ACTION.get("I" + i).keySet()) {
                    if (cell.getValue().equals(string)) {
                        bodyCell.add(new Cell(ACTION.get("I" + i).get(string)));
                    } else
                        cnt++;
                }
                if (cnt == ACTION.get("I" + i).keySet().size())
                    bodyCell.add(new Cell(""));
            }

            for (int j = analysis.terminals.size() + 1; j < header.size(); j++) {
                Cell cell = header.get(j);
                int cnt = 0;
                for (String string :
                        GOTO.get("I" + i).keySet()) {
                    if (cell.getValue().equals(string)) {
                        bodyCell.add(new Cell(GOTO.get("I" + i).get(string)));
                    } else
                        cnt++;
                }
                if (cnt == GOTO.get("I" + i).keySet().size())
                    bodyCell.add(new Cell(""));


            }


            body.add(bodyCell);
        }

        new ConsoleTable.ConsoleTableBuilder()
                .addHeaders(header)
                .addRows(body)
                .build()
                .print();
    }

    private void setGOTO() {
        //设置GOTO表：从一个状态转变到另一个状态，所需的输入符为非终结符
        //有多少个项目集，GOTO表就有多少行
        for (String key :
                analysis.itemSet.keySet()) {
            GOTO.put(key, new HashMap<>());
        }

        HashMap<String, String> tempGOTO;
        for (int i = 0; i < analysis.DFA.size(); i++) {
            tempGOTO = new HashMap<>();
            for (String key :
                    analysis.DFA.get("I" + i).keySet()) {
                //只有当项目的输入符号为非终结符时，才能添加到GOTO表中
                if (analysis.nonterminals.contains(key)) {
                    tempGOTO.put(key, analysis.DFA.get("I" + i).get(key));
                    GOTO.put("I" + i, tempGOTO);
                }
            }
        }

    }

    /**
     * 构造SLR分析表中的ACTION表
     */
    private void setACTION() {
        //ACTION表以HashMap的形式保存
        //<String, HashMap<String, String>>
        for (String key :
                analysis.itemSet.keySet()) {
            ACTION.put(key, new HashMap<>());
        }

        //当前项目的ACTION表项
        HashMap<String, String> tempACTION;
        //遍历每个项目集
        for (int i = 0; i < analysis.itemSet.size(); i++) {
            tempACTION = new HashMap<>();
            //分析当前项目集中的每个项目，获取每个项目的输入符号和对应的目标状态
            //例如E -> E. + E     则输入当前状态输入'+'，进入下一个状态
            for (String key : analysis.DFA.get("I" + i).keySet()) {
                //如果待输入符号是终结符
                if (analysis.terminals.contains(key)) {
                    if (isMovein(i, key)) {
                        //如果项目集i全为移入项目，那么显然没有矛盾
                        tempACTION.put(key, analysis.DFA.get("I" + i).get(key).replace("I", "S"));
                    } else if (isContradictory(analysis.itemSet.get("I" + i))) {
                        //如果有矛盾，就要进行处理 （既有移入项目又有归约项目或者有多个归约项目）
                        //项目集i的临时变量
                        List<Production> tempList = new ArrayList<>(analysis.itemSet.get("I" + i));
                        //找出项目集i中的移入项目，封装到movein当中
                        List<String> movein = new ArrayList<>();
                        //遍历项目集i中的所有移入项目
                        for (Production pro1 :
                                MoveinList) {
                            //如果待输入符号是终结符，那么可以移入    例如：E -> E.+T  那么显然待输入符号'+'是一个终结符，所以这个项目可以移入
                            if (analysis.terminals.contains(pro1.getRight()[pro1.getPosition()]))
                                movein.add(pro1.getRight()[pro1.getPosition()]);
                        }
                        for (String string : movein) {
                            tempACTION.put(string, analysis.DFA.get("I" + i).get(string).replace("I", "S"));
                        }

                        //遍历项目集i中的所有归约项目
                        for (Production pro2 :
                                GuiyueList) {
                            //只有输入符号属于归约项目产生式左部非终结符的follow集的非终结符才能进行归约！
                            for (String string : follows.get(pro2.getLeft())) {
                                tempACTION.put(string, "r" + analysis.getCount(pro2));
                            }
                        }
                    }
                }
            }
            //以下处理归约项目
            if (analysis.getCount((analysis.itemSet.get("I" + i).get(0))) == 0
                    && analysis.itemSet.get("I" + i).get(0).getPosition() == analysis.itemSet.get("I" + i).get(0).getRight().length) {
                tempACTION.put("#", "acc");
            } else if (analysis.DFA.get("I" + i).size() == 0) {
                for (String terminal : analysis.terminals) {
                    tempACTION.put(terminal, "r" + analysis.getCount(analysis.itemSet.get("I" + i).get(0)));
                }
            }
            ACTION.put("I" + i, tempACTION);
        }
    }


    /**
     * 判断移入、归约操作或者归约、归约操作是否矛盾
     *
     * @param list 项目集
     * @return true：有矛盾；false：没有矛盾
     */
    private boolean isContradictory(List<Production> list) {
        MoveinList = new ArrayList<>();
        GuiyueList = new ArrayList<>();
        //防止修改
        List<Production> tempList = new ArrayList<>(list);

        //遍历项目集中的项目
        for (Production production :
                tempList) {
            //如果这个项目是归约项目
            if (production.getPosition() == production.getRight().length) {
                GuiyueList.add(production);
            } else
                MoveinList.add(production);
        }
        //如果这个项目集中不止一个归约项目
        //或者这个项目集当中既有归约项目又有移入项目
        //那么这个项目集是移入、规约或者归约、归约矛盾的！
        return GuiyueList.size() > 1 || (GuiyueList.size() > 0 && MoveinList.size() > 0);
    }

    /**
     * 判断该项目集是否全为移入项目
     *
     * @param i 项目集i
     * @param key 项目key
     * @return true：能；false：不能
     */
    private boolean isMovein(int i, String key) {
        //遍历项目集i中的项目
        for (Production production :
                analysis.itemSet.get("I" + i)) {
            //如果有归约项目，那就不能直接作为移入项目集
            if (production.getPosition() == production.getRight().length)
                return false;
        }
        return true;
    }

    /**
     * 求非终结符的follow集
     *      在产生式集合中找产生式右部出现这个非终结符的产生式
     *      然后根据求follow集的规则求解
     */
    private void setFollows() {
        List<String> follow;
        for (String nonterminal :
                analysis.nonterminals) {
            follow = new ArrayList<>();
            follows.put(nonterminal, follow);
        }

        //首先将我们定义的 S'的follow集({#})手动添加到follows里
        follows.get(analysis.productions.get(0).getLeft()).add("#");

        //然后求其他非终结符的follow集
        for (String nonterminal :
                analysis.nonterminals) {
            follow = new ArrayList<>(setFollow(nonterminal));
            follows.get(nonterminal).removeAll(follow);
            follows.get(nonterminal).addAll(follow);
        }


    }

    /**
     * 求非终结符的follow集
     *
     * @param nonterminal 非终结符
     * @return follow集
     */
    private List<String> setFollow(String nonterminal) {
        //产生式的左部和右部
        String left;
        String[] rights;
        //将follows对应key上的value传入，这样可以一同修改
        List<String> follow = new ArrayList<>(follows.get(nonterminal));

        //求非终结符nonterminal的follow集步骤是在找到所有出现在产生式右部的这个非终结符，然后查看这个非终结符后是否跟着一个终结符
        //如果跟着一个终结符，就将此终结符加入到follow集中
        //如果跟着一个非终结符，则又分几种情况：
        //                                  1、非终结符的first集不含空，将非终结符的first集所有元素加入到follow集中
        //                                  2、非终结符的first集含空，则将first集去空加入到follow集中且再将这个产生式左部非终结符的follow集加入到follow集中
        for (Production production :
                analysis.productions) {
            left = production.getLeft();
            rights = production.getRight();
            for (int i = 0; i < rights.length; i++) {
                if (nonterminal.equals(rights[i])) {
                    if (i < rights.length - 1) {

                        List<String> first = new ArrayList<>(firsts.get(rights[i + 1]));
                        for (int temp = i + 1; temp < rights.length; temp++) {
                            first.removeAll(firsts.get(rights[temp]));
                            first.addAll(firsts.get(rights[temp]));
                            if (!first.contains("null")) {
                                break;
                            }
                        }

                        //2、非终结符的first集含空，则将first集去空加入到follow集中且再将这个产生式左部非终结符的follow集加入到follow集中
                        if (first.contains("null")) {
                            follow.removeAll(first);
                            follow.addAll(first);
                            follow.remove("null");
                            List<String> temp = setFollow(left);
                            follow.removeAll(temp);
                            follow.addAll(temp);
                        } else {
                            //1、非终结符的first集不含空，将非终结符的first集所有元素加入到follow集中
                            follow.removeAll(first);
                            follow.addAll(first);
                        }

                    } else if (i == rights.length - 1 && !rights[i].equals(left)) {
                        List<String> temp = setFollow(left);
                        follow.removeAll(temp);
                        follow.addAll(temp);
                    }

                }
            }
        }
        return follow;
    }

    /**
     * 构造每个非终结符的First集合
     * 定义每个终结符的First集合只含它自身
     */
    private void setFirsts() {
        List<String> first;
        //终结符的first集就只包含它本身
        for (String terminal :
                analysis.terminals) {
            first = new ArrayList<>();
            first.add(terminal);
            firsts.put(terminal, first);
        }

        //非终结符的first集需要进行运算
        for (String nonterminal :
                analysis.nonterminals) {
            first = new ArrayList<>();
            firsts.put(nonterminal, first);
        }

        for (String nonterminal :
                analysis.nonterminals) {
            //调用setFirst()来获得非终结符nonterminal的first集
            first = setFirst(nonterminal);
            //如果这个非终结符允许first集中包含 空字符，就将null添加到其对应的first集当中，但实际上我们这个文法并不允许
            if (CanBeNull.contains(nonterminal))
                first.add("null");
            firsts.put(nonterminal, first);
        }
    }

    /**
     * 求非终结符的first集
     *      到产生式集合中找到左部为这个非终结符的产生式
     *      然后分析这个产生式右部出现的第一个终结符
     *
     * @param nonterminal 非终结符
     * @return first集
     */
    private List<String> setFirst(String nonterminal) {
        String left;
        String[] rights;
        List<String> first = new ArrayList<>();

        for (Production production :
                analysis.productions) {
            //首先找到所有左部为nonterminal且右部第一个文法符号不为nonterminal这个非终结符的产生式
            if (production.getLeft().equals(nonterminal) && !production.getRight()[0].equals(nonterminal)) {
                left = nonterminal;
                rights = production.getRight();
                first = firsts.get(left);
                //如果这个产生式的右部第一个文法符号确实是终结符，那很简单就直接把这个终结符加入到first集当中就好了
                //如果这个产生式的右部第一个文法符号是非终结符，那么就要将这个非终结符的first集加入到当前非终结符的first集当中
                if (analysis.terminals.contains(rights[0])) {
                    if (!first.contains(rights[0]))
                        first.add(rights[0]);
                } else if (!CanBeNull.contains(rights[0])) {
                    //递归调用
                    List<String> temp = setFirst(rights[0]);
                    first.removeAll(temp);
                    first.addAll(temp);
                } else if (CanBeNull.contains(rights[0])) {
                    List<String> temp = setFirst(rights[0]);
                    temp.remove("null");
                    first.removeAll(temp);
                    first.addAll(temp);
                }
            }
        }
        return first;
    }
}
