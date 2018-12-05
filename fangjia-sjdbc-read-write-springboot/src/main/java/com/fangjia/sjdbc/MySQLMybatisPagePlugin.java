package com.fangjia.sjdbc;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MySQLMybatisPagePlugin extends MybatisPagePlugin {

    private static Logger log = LoggerFactory.getLogger(MySQLMybatisPagePlugin.class);

    @Override
    public String buildPageSql(String sql, PageUnsafe pageUnsafe) {
        StringBuilder sb = new StringBuilder();
        sb.append("select * from (");
        sb.append(sql);
        sb.append(") mybatis_page_temp_table");
        sb.append(" ");
        sb.append("limit");
        sb.append(" ");
        sb.append((pageUnsafe.getPageNo() - 1) * pageUnsafe.getPageSize());
        sb.append(",");
        sb.append(pageUnsafe.getPageSize());
        if (log.isInfoEnabled() && printSQL == 1) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("分页sql:" + System.lineSeparator());
            sb2.append(beautifySql(sb.toString()) + System.lineSeparator());
            sb2.append("分页参数:" + System.lineSeparator());
            sb2.append("pageNo:" + pageUnsafe.getPageNo() + System.lineSeparator());
            sb2.append("pageSize:" + pageUnsafe.getPageSize() + System.lineSeparator());
            log.info(sb2.toString());
            sb2 = null;
        }
        return sb.toString();
    }

    @Override
    public String buildPageCountSql(String sql) {
        StringBuilder sb = new StringBuilder();
        sb.append("select count(0) from (");
        sb.append(sql);
        sb.append(") mybatis_page_temp_table");
        if (log.isInfoEnabled() && printSQL == 1) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("统计分页总记录数sql:" + System.lineSeparator());
            sb2.append(beautifySql(sb.toString()) + System.lineSeparator());
            log.info(sb2.toString());
            sb2 = null;
        }
        return sb.toString();
    }


}
