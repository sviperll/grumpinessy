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

public class NoImportsOfHigherPackagesCheck extends AbstractCheck {
    private String packageName;

    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.IMPORT, TokenTypes.PACKAGE_DEF};
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {TokenTypes.IMPORT, TokenTypes.PACKAGE_DEF};
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {TokenTypes.IMPORT, TokenTypes.PACKAGE_DEF};
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        packageName = null;
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (ast.getType() == TokenTypes.PACKAGE_DEF) {
            packageName = readEnclosedDot(ast);
        } else if (ast.getType() == TokenTypes.IMPORT) {
            DetailAST dot = ast.findFirstToken(TokenTypes.DOT);
            if (dot != null) {
                String importedPackage = readEnclosedDot(dot);
                if (packageName != null && packageName.startsWith(importedPackage + ".")) {
                    log(ast, "import.of.higher.package", importedPackage, packageName);
                }
            }
        }
    }

    private String readEnclosedDot(DetailAST ast) {
        DottedIdentifierReader reader = new DottedIdentifierReader();
        reader.appendEnclosedDot(ast);
        return reader.toString();
    }

    private static class DottedIdentifierReader {

        private final StringBuilder builder = new StringBuilder();

        private void appendEnclosedDot(DetailAST ast) {
            DetailAST dot = ast.findFirstToken(TokenTypes.DOT);
            if (dot != null) {
                appendDot(dot);
            } else {
                DetailAST ident = ast.findFirstToken(TokenTypes.IDENT);
                if (ident != null) {
                    builder.append(ident.getText());
                }
            }
        }

        private void appendDot(DetailAST ast) {
            DetailAST dot = ast.findFirstToken(TokenTypes.DOT);
            if (dot != null) {
                appendDot(dot);
                builder.append(".");
            }
            DetailAST ident = ast.findFirstToken(TokenTypes.IDENT);
            if (ident != null) {
                builder.append(ident.getText());
            }
        }

        @Override
        public String toString() {
            return builder.toString();
        }

    }

}
