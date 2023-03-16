import java.util.Arrays;
import java.util.Objects;

/**
 * 产生式数据结构
 *
 * @author 沈慧昌
 * @date 2022年12月22日12:07:41
 */
public class Production {
    //产生式左部
    private String left;
    //产生式右部
    private String[] right;
    //产生式：例如S'->S
    private String string;
    //记录了每个产生式'.'所在的位置      例如 E -> E +. E  那么这个产生式的position = 2
    private int position;


    public Production(String left, String[] right, String string) {
        this.left = left;
        this.right = right;
        this.string = string;
        position = 0;
    }

    public Production(Production production, int position) {
        this.string = production.getString();
        this.left = production.getLeft();
        this.right = production.getRight();
        this.position = position;
    }


    public String getLeft() {
        return left;
    }

    public String[] getRight() {
        return right;
    }

    public String getString() {
        return string;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return left + " -> " + Arrays.toString(right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return position == that.position &&
                Objects.equals(left, that.left) &&
                Arrays.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(left, string, position);
        result = 31 * result + Arrays.hashCode(right);
        return result;
    }
}
