/**
 * 三地址码类
 *
 * @author 沈慧昌
 * @date 2022年12月21日17:29:21
 */
public class TAC {
    //三地址码编号
    private String val;

    //三地址码表达式
    private String expr;

    public TAC(String expr) {
        this.expr = expr;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public TAC() {

    }


}
