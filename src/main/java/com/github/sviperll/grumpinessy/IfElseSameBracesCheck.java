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

public class IfElseSameBracesCheck extends AbstractCheck {

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.LITERAL_ELSE,
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
    public void visitToken(DetailAST elseNode) {
        DetailAST ifNode = elseNode.getParent();
        boolean isElseIf = elseNode.getFirstChild().getType() == TokenTypes.LITERAL_IF;
        DetailAST alternative = isElseIf ? elseNode.getFirstChild() : elseNode;
        boolean ifHasBraces = ifNode.findFirstToken(TokenTypes.SLIST) != null;
        boolean alternativeHasBraces = alternative.findFirstToken(TokenTypes.SLIST) != null;
        if (ifHasBraces != alternativeHasBraces) {
            log(elseNode, "if.else.should.both.have.braces");
        }
    }
}
