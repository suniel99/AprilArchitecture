/*
 * Created on 05/10/2006
 */
package gipsy.GIPC.intensional.SIPL.JOOIP.ast.expr;

import gipsy.GIPC.intensional.SIPL.JOOIP.ast.visitor.GenericVisitor;
import gipsy.GIPC.intensional.SIPL.JOOIP.ast.visitor.VoidVisitor;

/**
 * @author Julio Vilmar Gesser
 */
public final class ArrayAccessExpr extends Expression {

    public final Expression name;

    public final Expression index;

    public ArrayAccessExpr(int line, int column, Expression name, Expression index) {
        super(line, column);
        this.name = name;
        this.index = index;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

}
