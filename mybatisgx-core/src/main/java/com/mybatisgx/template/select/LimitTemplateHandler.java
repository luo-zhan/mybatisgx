package com.mybatisgx.template.select;

import com.mybatisgx.ext.session.MybatisgxConfiguration;
import com.mybatisgx.model.MethodRowLimitInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分页模板处理器
 * @author 薛承城
 * @date 2025/11/22 19:16
 */
public class LimitTemplateHandler {

    private static final Map<String, MethodRowLimitHandler> METHOD_ROW_LIMIT_MAP = new ConcurrentHashMap<>();
    private final MybatisgxConfiguration configuration;

    public LimitTemplateHandler(MybatisgxConfiguration configuration) {
        this.configuration = configuration;
        register("MySQL", new LimitTemplateHandler.MysqlMethodRowLimitHandler());
        register("Oracle", new LimitTemplateHandler.OracleMethodRowLimitHandler());
        register("PostgreSQL", new LimitTemplateHandler.PgsqlMethodRowLimitHandler());
    }

    public void execute(List<Object> selectXmlItemList, MethodRowLimitInfo methodRowLimitInfo) {
        MethodRowLimitHandler methodRowLimitHandler = METHOD_ROW_LIMIT_MAP.get(this.configuration.getDatabaseId());
        methodRowLimitHandler.apply(selectXmlItemList, methodRowLimitInfo);
    }

    public static class MysqlMethodRowLimitHandler implements MethodRowLimitHandler {

        private static final String LIMIT_SQL_EXPRESSION = " limit %s, %s";

        public void apply(List<Object> selectXmlItemList, MethodRowLimitInfo methodRowLimitInfo) {
            Integer offset = methodRowLimitInfo.getOffset();
            Integer size = methodRowLimitInfo.getSize();
            String limitSqlExpression = String.format(LIMIT_SQL_EXPRESSION, offset * size, size);
            selectXmlItemList.add(limitSqlExpression);
        }
    }

    public static class OracleMethodRowLimitHandler implements MethodRowLimitHandler {

        private static final String LIMIT_SQL_EXPRESSION_START = "SELECT * FROM (SELECT t.*, ROWNUM AS rn FROM (";
        private static final String LIMIT_SQL_EXPRESSION_END = ") t WHERE ROWNUM <= %s) WHERE rn > %s";

        public void apply(List<Object> selectXmlItemList, MethodRowLimitInfo methodRowLimitInfo) {
            selectXmlItemList.add(0, LIMIT_SQL_EXPRESSION_START);
            Integer offset = methodRowLimitInfo.getOffset();
            Integer size = methodRowLimitInfo.getSize();
            String limitSqlExpressionEnd = String.format(LIMIT_SQL_EXPRESSION_END, (offset + 1) * size, offset * size);
            selectXmlItemList.add(limitSqlExpressionEnd);
        }
    }

    public static class PgsqlMethodRowLimitHandler implements MethodRowLimitHandler {

        private static final String LIMIT_SQL_EXPRESSION = " limit %s OFFSET %s";

        public void apply(List<Object> selectXmlItemList, MethodRowLimitInfo methodRowLimitInfo) {
            Integer offset = methodRowLimitInfo.getOffset();
            Integer size = methodRowLimitInfo.getSize();
            String limitSqlExpression = String.format(LIMIT_SQL_EXPRESSION, size, offset * size);
            selectXmlItemList.add(limitSqlExpression);
        }
    }

    public static void register(String databaseId, MethodRowLimitHandler methodRowLimitHandler) {
        METHOD_ROW_LIMIT_MAP.put(databaseId.toLowerCase(), methodRowLimitHandler);
    }
}
