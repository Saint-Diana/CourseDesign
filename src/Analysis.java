import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 采用SLR(1)自底向上语法分析方法
 *      构造SLR(1)项目集族
 *
 * @author 沈慧昌
 * @date 2022年12月23日08:32:15
 */
public class Analysis {
    //项目集，记录了文法对应DFA中的所有项目集，项目集的形式是   <Ix, {当前项目集Ix中的所有产生式}>
    HashMap<String, List<Production>> itemSet;
    //词法分析结果
    Lex lex;
    //产生式集合
    List<Production> productions;
    //终结符
    List<String> terminals;
    //非终结符
    List<String> nonterminals;
    //用于判断闭包运算是否结束
    List<Production> endList;
    private final List<Object> leftEnd;
    private final List<Object> rightEnd;
    //语法分析构造的DFA，DFA的key是状态；value也是一个HashMap，value的key是输入符号；value的value是当前状态接受输入符号后的目标状态
    //DFA的value的key组成的集合就是当前项目集的输入符号集
    HashMap<String, HashMap<String, String>> DFA;


    public Analysis(Lex lex) {
        this.lex = lex;
        terminals = new ArrayList<>();
        nonterminals = new ArrayList<>();
        productions = new ArrayList<>();
        itemSet = new HashMap<>();
        endList = new ArrayList<>();
        leftEnd = new ArrayList<>();
        rightEnd = new ArrayList<>();
        DFA = new HashMap<>();

        //读取文法文件，在设置产生式集合productions的同时，根据产生式的左部设置非终结符集nonterminals
        readFile();
        //设置终结符集terminals
        setTerminals();
        //根据文法生成SLR(1)项目集族
        setItemSet();
    }


    private void readFile() {
        try {
            //读取文法文件"Grammar"
            InputStream is = Files.newInputStream(Paths.get("Grammar"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            //文法产生式左部
            String left;
            //文法产生式右部
            String[] rights;
            //读取的一行产生式
            String line;
            //遍历整个文法
            while ((line = reader.readLine()) != null) {
                left = line.split("->")[0].trim();
                String s = line.split("->")[1];
                rights = s.trim().split("\\|");
                //因为一行产生式实际上可能是多个产生式的缩略写法，所以我们这里需要将其展开为多个产生式添加到productions集合中
                for (String right : rights) {
                    Production production = new Production(left, right.trim().split(" "), left + " -> " + right.trim());
                    productions.add(production);
                }
                //因为产生式左部肯定是非终结符，所以如果这条产生式的左部非终结符不在非终结符集里的话，就将其添加进去。
                if (!nonterminals.contains(left)) {
                    nonterminals.add(left);
                }
            }
            reader.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTerminals() {
        //根据产生式的右部来设置终结符集
        String[] right;
        for (Production production :
                productions) {
            //获取每条产生式的右部
            right = production.getRight();
            for (String st :
                    right) {
                //如果不是非终结符且终结符集中不含该终结符，则将其添加进去！
                if (!(nonterminals.contains(st)) && !terminals.contains(st))
                    terminals.add(st);
            }
        }
        //额外需要添加一个"#"作为输入串的结束符
        terminals.add("#");
    }

    /**
     * 构造项目集
     */
    private void setItemSet() {
        //对基本项目集进行闭包运算
        List<Production> closure = new ArrayList<>();
        //产生式右部
        List<String> rights;
        //产生式
        Production production;
        //首先构造I0，需要先手动找到开始符为产生式左部的产生式，然后将其加入闭包
        //遍历每一条产生式，如果产生式的左部是文法的开始符S'，则这些产生式是基本项目集中的元素，所以需要添加到closure中！
        for (Production pro :
                productions) {
            if (pro.getLeft().equals(productions.get(0).getLeft())) {
                closure.add(pro);
            }
        }
        itemSet.put("I0", closure);
        //进行闭包运算
        for (int i = 0; i < itemSet.size(); i++) {
            closure = itemSet.get("I" + i);
            //记录闭包中所有项目的左部非终结符
            rights = new ArrayList<>();
            for (int j = 0; j < closure.size(); j++) {
                //闭包中的第j条产生式/项目
                production = closure.get(j);
                //如果不是归约项目
                if (production.getPosition() != production.getRight().length) {
                    //找到'.'后的那个字符，然后判断该字符是否为非终结符，如果是非终结符且未处理过，就将以这个非终结符为产生式左部的所有产生式都添加到闭包中！
                    //直到所有相关的项目都加入闭包为止
                    String right_pos = production.getRight()[production.getPosition()];
                    if (nonterminals.contains(right_pos) && !rights.contains(right_pos)) {
                        closure.addAll(getProdByLeft(right_pos));
                        rights.add(right_pos);
                    }
                }
            }

            rights = new ArrayList<>();
            //遍历闭包中的项目
            for (Production value : closure) {
                //如果不是归约项目且当前项目还未处理
                if (value.getPosition() != value.getRight().length && !rights.contains(value.getRight()[value.getPosition()])) {
                    rights.add(value.getRight()[value.getPosition()]);
                }
            }

            HashMap<String, String> DFAString = new HashMap<>();
            for (String right : rights) {
                List<Production> temp = new ArrayList<>();
                //现在闭包中已经加入了基本项目，开始进行运算。
                for (Production pro : closure) {
                    if (pro.getPosition() != pro.getRight().length && pro.getRight()[pro.getPosition()].equals(right)) {
                        Production production1 = new Production(pro, pro.getPosition());
                        if (production1.getPosition() != production1.getRight().length) {
                            production1.setPosition(production1.getPosition() + 1);
                            temp.add(production1);
                        }
                    }

                }
                if (temp.size() > 0) {
                    if (!isExist(temp)) {
                        int size = itemSet.size();
                        itemSet.put("I" + size, temp);
                        DFAString.put(right, "I" + size);
                    } else {
                        String id = getID(temp);
                        DFAString.put(right, id);
                    }
                }
            }
            DFA.put("I" + i, DFAString);
            isEnd();
        }
    }

    private List<Production> getItem(List<Production> temp) {
        for (List<Production> list :
                itemSet.values()) {
            for (int i = 0; i < list.size(); i++) {
                if (!list.get(i).equals(temp.get(i)))
                    break;
                if (i == list.size() - 1) {
                    return list;
                }
            }
        }
        return null;
    }

    private void isEnd() {
        for (List<Production> tempList : itemSet.values()) {
            for (Production production : tempList) {
                if (production.getPosition() == production.getRight().length) {
                    if (!(leftEnd.contains(production.getLeft()) && rightEnd.contains(production.getRight()))) {
                        leftEnd.add(production.getLeft());
                        rightEnd.add(production.getRight());
                        endList.add(production);
                    }
                }
            }
        }
    }

    /**
     * 根据产生式左部非终结符，获取所有产生式左部为left的产生式集合
     *
     * @param left 产生式左部非终结符
     * @return 所有产生式左部为left的产生式集合
     */
    private List<Production> getProdByLeft(String left) {
        List<Production> productionList = new ArrayList<>();
        for (Production production : productions) {
            if (production.getLeft().equals(left)) {
                productionList.add(production);
            }
        }
        return productionList;
    }


    public int getCount(Production production) {
        //返回产生式production在产生式集合productions中的位置！
        for (Production pro : productions) {
            if (pro.getLeft().equals(production.getLeft()) && Arrays.equals(pro.getRight(), production.getRight())) {
                return productions.indexOf(pro);
            }
        }
        return -1;
    }

    public String getID(List<Production> production) {
        for (String key :
                itemSet.keySet()) {
            int count = 0;
            for (int i = 0; i < itemSet.get(key).size(); i++) {
                for (Production value : production) {
                    if (itemSet.get(key).get(i).equals(value)) {
                        count++;
                    }
                }

            }

            if (count == production.size()) {
                return key;
            }

        }
        return null;
    }

    private boolean isExist(List<Production> temp) {

        for (List<Production> list :
                itemSet.values()) {
            if (list.get(0).equals(temp.get(0))) {
                return true;
            }
        }
        return false;

    }


}
