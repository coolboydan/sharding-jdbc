/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.parsing.antlr.mysql.ddl;

import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.parsing.antler.parser.MySQLStatementAdvancedParser;
import io.shardingsphere.core.parsing.antler.parser.PostgreStatementAdvancedParser;
import io.shardingsphere.core.parsing.antler.parser.factory.StatementFactory;
import io.shardingsphere.core.parsing.integrate.asserts.AntlrParserResultSetLoader;
import io.shardingsphere.core.parsing.integrate.asserts.SQLStatementAssert;
import io.shardingsphere.core.parsing.integrate.engine.AbstractBaseIntegrateSQLParsingTest;
import io.shardingsphere.parser.antlr.MySQLStatementLexer;
import io.shardingsphere.parser.antlr.PostgreStatementLexer;
import io.shardingsphere.test.sql.AntlrSQLCasesLoader;
import io.shardingsphere.test.sql.SQLCaseType;
import io.shardingsphere.test.sql.SQLCasesLoader;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@RunWith(Parameterized.class)
@RequiredArgsConstructor
public final class MySQLDDLParsingTest extends AbstractBaseIntegrateSQLParsingTest{
    
    private static SQLCasesLoader sqlCasesLoader = AntlrSQLCasesLoader.getInstance();
    
    private static AntlrParserResultSetLoader parserResultSetLoader = AntlrParserResultSetLoader.getInstance();
    
    private final String sqlCaseId;
    
    private final DatabaseType databaseType;
    
    private final SQLCaseType sqlCaseType;
    
    @Parameterized.Parameters(name = "{0} ({2}) -> {1}")
    public static Collection<Object[]> getTestParameters() {
        return sqlCasesLoader.getSupportedSQLTestParameters(Arrays.<Enum>asList(DatabaseType.values()), DatabaseType.class);
    }
    
    @Test
    public void parsingSupportedSQL() throws Exception {
        String sql = sqlCasesLoader.getSupportedSQL(sqlCaseId, sqlCaseType, Collections.emptyList());
        CodePointCharStream cs = CharStreams.fromString(sql);
        switch (databaseType) {
            case MySQL:
                execute(MySQLStatementLexer.class, MySQLStatementAdvancedParser.class,  cs);
                break;
            case Oracle:
                break;
            case PostgreSQL:
                execute(PostgreStatementLexer.class, PostgreStatementAdvancedParser.class,  cs);
                break;
            case SQLServer:
                break;
            default:
                break;
        }
    }
    
    @Test
    public void assertSupportedSQL() {
        String sql = sqlCasesLoader.getSupportedSQL(sqlCaseId, sqlCaseType, parserResultSetLoader.getParserResult(sqlCaseId).getParameters());
        new SQLStatementAssert(StatementFactory.getStatement(databaseType, null, getShardingRule(), sql, getShardingTableMetaData()), sqlCaseId, sqlCaseType, AntlrSQLCasesLoader.getInstance(), AntlrParserResultSetLoader.getInstance()).assertSQLStatement();
    }
    
    public void execute(Class<?> lexerClass, Class<?> parserClass, CharStream cs) throws Exception {
        Constructor<?> lexerCon = lexerClass.getConstructor(new Class[] {CharStream.class});
        Lexer lexer = (Lexer)lexerCon.newInstance(new Object[] {cs});
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        Constructor<?> parserCon = parserClass.getConstructor(new Class[] {TokenStream.class});
        Object parser = parserCon.newInstance(new Object[] {tokenStream});
        Method addErrorListener = parserClass.getMethod("addErrorListener", ANTLRErrorListener.class);
        addErrorListener.invoke(parser, new BaseErrorListener() {
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException();
            }
        });
        Method execute = parserClass.getMethod("execute");
        execute.invoke(parser, new Object[] {});
    }
}
