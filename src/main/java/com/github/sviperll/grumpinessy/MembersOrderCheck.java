/*
 * #%L
 * %%
 * Copyright (C) 2023 Victor Nazarov <asviraspossible@gmail.com>
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.github.sviperll.grumpinessy;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MembersOrderCheck extends AbstractCheck {

    private int staticVariableOrdinal = 1;
    private int staticInitializerOrdinal = 2;
    private int staticMethodOrdinal = 3;
    private int instanceVariableOrdinal = 4;
    private int constructorOrdinal = 5;
    private int instanceInitializerOrdinal = 6;
    private int instanceMethodOrdinal = 7;
    private int innerClassOrdinal = 8;
    private int staticNetstedClassOrdinal = 9;
    private InitializedCheck initializedCheck = null;

    public void setStaticVariableOrdinal(int staticVariableOrdinal) {
        this.staticVariableOrdinal = staticVariableOrdinal;
    }

    public void setStaticInitializerOrdinal(int staticInitializerOrdinal) {
        this.staticInitializerOrdinal = staticInitializerOrdinal;
    }

    public void setStaticMethodOrdinal(int staticMethodOrdinal) {
        this.staticMethodOrdinal = staticMethodOrdinal;
    }

    public void setInstanceVariableOrdinal(int instanceVariableOrdinal) {
        this.instanceVariableOrdinal = instanceVariableOrdinal;
    }

    public void setConstructorOrdinal(int constructorOrdinal) {
        this.constructorOrdinal = constructorOrdinal;
    }

    public void setInstanceInitializerOrdinal(int instanceInitializerOrdinal) {
        this.instanceInitializerOrdinal = instanceInitializerOrdinal;
    }

    public void setInstanceMethodOrdinal(int instanceMethodOrdinal) {
        this.instanceMethodOrdinal = instanceMethodOrdinal;
    }

    public void setInnerClassOrdinal(int innerClassOrdinal) {
        this.innerClassOrdinal = innerClassOrdinal;
    }

    public void setStaticNetstedClassOrdinal(int staticNetstedClassOrdinal) {
        this.staticNetstedClassOrdinal = staticNetstedClassOrdinal;
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.CLASS_DEF,
            TokenTypes.INTERFACE_DEF,
            TokenTypes.ANNOTATION_DEF,
            TokenTypes.ENUM_DEF,
            TokenTypes.RECORD_DEF,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.STATIC_INIT,
            TokenTypes.CTOR_DEF,
            TokenTypes.COMPACT_CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.VARIABLE_DEF,
            TokenTypes.OBJBLOCK
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    @Override
    public void init() {
        Map<Positioned, Integer> builder = new HashMap<>();
        builder.put(new Positioned(true, Kind.VARIABLE), staticVariableOrdinal);
        builder.put(new Positioned(true, Kind.INITIALIZER), staticInitializerOrdinal);
        builder.put(new Positioned(true, Kind.METHOD), staticMethodOrdinal);
        builder.put(new Positioned(false, Kind.VARIABLE), instanceVariableOrdinal);
        builder.put(new Positioned(false, Kind.CONSTRUCTOR), constructorOrdinal);
        builder.put(new Positioned(false, Kind.INITIALIZER), instanceInitializerOrdinal);
        builder.put(new Positioned(false, Kind.METHOD), instanceMethodOrdinal);
        builder.put(new Positioned(false, Kind.CLASS), innerClassOrdinal);
        builder.put(new Positioned(true, Kind.CLASS), staticNetstedClassOrdinal);
        Map<Positioned, Integer> mapping = Map.copyOf(builder);
        Comparator<Positioned> comparator = Comparator.comparingInt(mapping::get);
        String order =
                mapping.keySet()
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        mapping::get,
                                        Collectors.mapping(
                                                Object::toString,
                                                Collectors.joining(" or ")
                                        )
                                )
                        )
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparing(Entry::getKey))
                        .map(Entry::getValue)
                        .collect(Collectors.joining("; then "));
        initializedCheck = new InitializedCheck(this, comparator, order);
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        initializedCheck.beginTree(rootAST);
    }

    @Override
    public void visitToken(DetailAST ast) {
        initializedCheck.visitToken(ast);
    }

    @Override
    public void leaveToken(DetailAST ast) {
        initializedCheck.leaveToken(ast);
    }

    private static class InitializedCheck {
        private final ArrayDeque<StackFrame> stack = new ArrayDeque<>();
        private Positioned previous = null;
        private boolean parentForcesVariablesToBeStatic = false;
        private final AbstractCheck reporter;
        private final Comparator<Positioned> comparator;
        private final String order;

        private InitializedCheck(
                AbstractCheck reporter,
                Comparator<Positioned> comparator,
                String order
        ) {
            this.reporter = reporter;
            this.comparator = comparator;
            this.order = order;
        }

        void beginTree(DetailAST rootAST) {
            previous = null;
            parentForcesVariablesToBeStatic = false;
        }

        void visitToken(DetailAST ast) {
            if (ast.getType() == TokenTypes.OBJBLOCK) {
                stack.push(new StackFrame(previous, parentForcesVariablesToBeStatic));
                previous = null;
                DetailAST parent = ast.getParent();
                parentForcesVariablesToBeStatic =
                        parent != null && (
                                parent.getType() == TokenTypes.INTERFACE_DEF
                                || parent.getType() == TokenTypes.ANNOTATION_DEF
                        );
                return;
            }
            DetailAST parent = ast.getParent();
            if (parent == null || parent.getType() != TokenTypes.OBJBLOCK)
                return;
            Positioned current = toPositioned(ast);
            if (previous != null && comparator.compare(previous, current) > 0) {
                reporter.log(ast, "wrong.member.order", previous, current, order);
            }
            previous = current;
        }

        void leaveToken(DetailAST ast) {
            if (ast.getType() == TokenTypes.OBJBLOCK) {
                StackFrame stackFrame = stack.pop();
                previous = stackFrame.previous();
                parentForcesVariablesToBeStatic = stackFrame.parentForcesVariablesToBeStatic;
            }
        }

        private Positioned toPositioned(DetailAST ast) {
            return new Positioned(isStatic(ast), Kind.fromAst(ast));
        }

        private boolean isStatic(DetailAST ast) {
            return switch (ast.getType()) {
                case TokenTypes.CLASS_DEF ->
                        parentForcesVariablesToBeStatic || hasStaticModifier(ast);
                case TokenTypes.INTERFACE_DEF -> true;
                case TokenTypes.ANNOTATION_DEF -> true;
                case TokenTypes.ENUM_DEF -> true;
                case TokenTypes.RECORD_DEF -> true;
                case TokenTypes.INSTANCE_INIT -> false;
                case TokenTypes.STATIC_INIT -> true;
                case TokenTypes.CTOR_DEF -> false;
                case TokenTypes.COMPACT_CTOR_DEF -> false;
                case TokenTypes.METHOD_DEF -> hasStaticModifier(ast);
                case TokenTypes.VARIABLE_DEF ->
                        parentForcesVariablesToBeStatic || hasStaticModifier(ast);
                default -> throw new UnsupportedOperationException(
                        String.format("Unknown syntax node: %s: %s", ast.getType(), ast.getText())
                );
            };
        }

        private boolean hasStaticModifier(DetailAST ast) {
            DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
            return modifiers != null && modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) != null;
        }

        record StackFrame(Positioned previous, boolean parentForcesVariablesToBeStatic) {}
    }

    enum Kind {
        VARIABLE, INITIALIZER, CONSTRUCTOR, METHOD, CLASS;

        static Kind fromAst(DetailAST ast) {
            return switch (ast.getType()) {
                case TokenTypes.CLASS_DEF -> Kind.CLASS;
                case TokenTypes.INTERFACE_DEF -> Kind.CLASS;
                case TokenTypes.ANNOTATION_DEF -> Kind.CLASS;
                case TokenTypes.ENUM_DEF -> Kind.CLASS;
                case TokenTypes.RECORD_DEF -> Kind.CLASS;
                case TokenTypes.INSTANCE_INIT -> Kind.INITIALIZER;
                case TokenTypes.STATIC_INIT -> Kind.INITIALIZER;
                case TokenTypes.CTOR_DEF -> Kind.CONSTRUCTOR;
                case TokenTypes.COMPACT_CTOR_DEF -> Kind.CONSTRUCTOR;
                case TokenTypes.METHOD_DEF -> Kind.METHOD;
                case TokenTypes.VARIABLE_DEF -> Kind.VARIABLE;
                default -> throw new UnsupportedOperationException(
                        String.format("Unknown syntax node: %s: %s", ast.getType(), ast.getText())
                );
            };
        }
    }

    record Positioned(boolean isStatic, Kind kind) {
        @Override
        public String toString() {
            return switch (kind) {
                case VARIABLE -> (isStatic ? "static " : "") + "variable";
                case INITIALIZER -> (isStatic ? "static " : "") + "initializer";
                case CONSTRUCTOR -> "constructor";
                case METHOD -> (isStatic ? "static " : "") + "method";
                case CLASS -> isStatic
                    ? "static nested class or nested interface, nested enum or nested record"
                    : "inner class";
            };
        }
    }
}
