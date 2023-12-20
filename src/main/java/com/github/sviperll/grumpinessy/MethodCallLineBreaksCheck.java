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

public class MethodCallLineBreaksCheck extends AbstractCheck {

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.METHOD_CALL,
            TokenTypes.LITERAL_NEW,
            TokenTypes.METHOD_DEF,
            TokenTypes.CTOR_DEF,
            TokenTypes.RECORD_DEF
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
    public void visitToken(DetailAST ast) {
        int leftParensLine = getLeftParens(ast);
        int rightParensLine = getRightParens(ast);
        if (leftParensLine != rightParensLine) {
            int expectedLineNo = leftParensLine + 1;
            DetailAST parameters = getParameters(ast);
            if (parameters != null) {
                DetailAST parameter = parameters.getFirstChild();
                while (parameter != null) {
                    if (parameter.getLineNo() < expectedLineNo) {
                        log(
                                ast,
                                "multiple.arguments.on.one.line",
                                parameter.getLineNo(),
                                expectedLineNo,
                                leftParensLine,
                                rightParensLine
                        );
                        return;
                    }
                    expectedLineNo = parameter.getLineNo() + 1;
                    parameter = parameter.getNextSibling();
                    if (parameter != null && parameter.getType() == TokenTypes.COMMA) {
                        expectedLineNo = parameter.getLineNo() + 1;
                        parameter = parameter.getNextSibling();
                    }
                }
            }
            if (rightParensLine < expectedLineNo) {
                log(
                        ast,
                        "multiple.arguments.on.one.line",
                        rightParensLine,
                        expectedLineNo,
                        leftParensLine,
                        rightParensLine
                );
            }
        }
    }

    private int getLeftParens(DetailAST ast) {
        DetailAST lparen = ast.findFirstToken(TokenTypes.LPAREN);
        return lparen == null ? ast.getLineNo() : lparen.getLineNo();
    }

    private int getRightParens(DetailAST ast) {
        DetailAST rparen = ast.findFirstToken(TokenTypes.RPAREN);
        return rparen == null ? ast.getLineNo() : rparen.getLineNo();
    }

    private DetailAST getParameters(DetailAST ast) {
        return switch (ast.getType()) {
            case TokenTypes.METHOD_CALL, TokenTypes.LITERAL_NEW ->
                ast.findFirstToken(TokenTypes.ELIST);
            case TokenTypes.METHOD_DEF, TokenTypes.CTOR_DEF ->
                ast.findFirstToken(TokenTypes.PARAMETERS);
            case TokenTypes.RECORD_DEF -> ast.findFirstToken(TokenTypes.RECORD_COMPONENTS);
            default -> throw new UnsupportedOperationException(
                    String.format("Unsupported syntax %s: %s", ast.getType(), ast.getText())
            );
        };
    }

}
