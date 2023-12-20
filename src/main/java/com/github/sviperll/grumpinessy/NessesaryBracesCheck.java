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
import java.util.Objects;
import java.util.stream.Stream;

public class NessesaryBracesCheck extends AbstractCheck {

    private static boolean isMultilineNode(DetailAST node) {
        LineSpan span = getLineSpan(node);
        return span.start() < span.end();
    }

    private static LineSpan getLineSpan(DetailAST node) {
        LineSpan span = new LineSpan(node.getLineNo(), node.getLineNo());
        return Stream.iterate(node.getFirstChild(), Objects::nonNull, DetailAST::getNextSibling)
                .map(NessesaryBracesCheck::getLineSpan)
                .reduce(span, LineSpan::cover);
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_ELSE,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_WHILE
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
    public void visitToken(DetailAST node) {
        DetailAST body = switch (node.getType()) {
            case TokenTypes.LITERAL_IF, TokenTypes.LITERAL_FOR, TokenTypes.LITERAL_WHILE ->
                    node.findFirstToken(TokenTypes.RPAREN).getNextSibling();
            case TokenTypes.LITERAL_ELSE -> node.getFirstChild();
            default ->
                    throw new UnsupportedOperationException(
                            "unsupported node type: " + node.getType()
                    );
        };
        boolean isElseIf =
                node.getType() == TokenTypes.LITERAL_ELSE
                        && body.getType() == TokenTypes.LITERAL_IF;
        if (!isElseIf && body.getType() != TokenTypes.SLIST && isMultilineNode(body)) {
            log(body, "braces.are.mandatory.for.multiline");
        }
    }

    record LineSpan(int start, int end) {
        static LineSpan cover(LineSpan span1, LineSpan span2) {
            int start = Math.min(span1.start(), span2.start());
            int end = Math.max(span1.end(), span2.end());
            return new LineSpan(start, end);
        }
    }
}
