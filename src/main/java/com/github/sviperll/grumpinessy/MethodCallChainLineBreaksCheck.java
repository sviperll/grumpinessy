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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MethodCallChainLineBreaksCheck extends AbstractCheck {
    private static CodeSpan getCodeSpan(DetailAST ast) {
        Location location = new Location(ast.getLineNo(), ast.getColumnNo());
        CodeSpan span = new CodeSpan(location, location);
        return Stream.iterate(ast.getFirstChild(), Objects::nonNull, DetailAST::getNextSibling)
                .map(MethodCallChainLineBreaksCheck::getCodeSpan)
                .reduce(span, CodeSpan::cover);
    }

    private static boolean isMethodCall(DetailAST ast) {
        return ast.getType() == TokenTypes.METHOD_CALL;
    }

    private static int getDotLine(DetailAST call) {
        return call.findFirstToken(TokenTypes.DOT).getLineNo();
    }

    private static DetailAST getTarget(DetailAST call) {
        DetailAST dot = call.findFirstToken(TokenTypes.DOT);
        return dot == null ? null : dot.getFirstChild();
    }

    private static boolean isMultilineCall(DetailAST call) {
        DetailAST dot = call.findFirstToken(TokenTypes.DOT);
        DetailAST rparen = call.findFirstToken(TokenTypes.RPAREN);
        return dot != null && rparen != null && dot.getLineNo() != rparen.getLineNo();
    }

    private static boolean hasDot(DetailAST ast) {
        return ast.findFirstToken(TokenTypes.DOT) != null;
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.METHOD_CALL};
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
        List<DetailAST> calls = new ArrayList<>();
        while (ast != null && isMethodCall(ast) && hasDot(ast) && calls.size() < 3) {
            calls.add(ast);
            ast = getTarget(ast);
        }
        if (calls.size() == 2 && getDotLine(calls.get(0)) != getDotLine(calls.get(1))) {
            DetailAST call = calls.get(1);
            DetailAST dot = call.findFirstToken(TokenTypes.DOT);
            DetailAST target = getTarget(call);
            CodeSpan span = getCodeSpan(target);
            if (isMultilineCall(call) && span.end().line() == dot.getLineNo()) {
                log(call, "line.break.is.required.complex.first.method.call.in.chain");
            }
        }
        if (calls.size() < 3)
            return;
        int[] callLines =
                calls.stream()
                        .mapToInt(MethodCallChainLineBreaksCheck::getDotLine)
                        .toArray();
        if (callLines[0] == callLines[1] && callLines[1] != callLines[2]) {
            log(calls.get(0), "multiple.method.calls.in.chain.on.same.line");
        }
        if (callLines[0] != callLines[1] && callLines[1] == callLines[2]) {
            log(calls.get(1), "multiple.method.calls.in.chain.on.same.line");
        }
    }

    record Location(int line, int column) implements Comparable<Location> {
        private static final Comparator<Location> COMPARATOR =
                Comparator.comparingInt(Location::line).thenComparingInt(Location::column);
        static Location min(Location location1, Location location2) {
            return location1.compareTo(location2) <= 0 ? location1 : location2;
        }

        static Location max(Location location1, Location location2) {
            return location1.compareTo(location2) >= 0 ? location1 : location2;
        }

        @Override
        public int compareTo(Location that) {
            return COMPARATOR.compare(this, that);
        }
    }
    record CodeSpan(Location start, Location end) {
        static CodeSpan cover(CodeSpan span1, CodeSpan span2) {
            Location start = Location.min(span1.start(), span2.start());
            Location end = Location.max(span1.end(), span2.end());
            return new CodeSpan(start, end);
        }
    }
}
