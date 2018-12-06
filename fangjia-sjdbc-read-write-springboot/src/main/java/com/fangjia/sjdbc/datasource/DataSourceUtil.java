package com.fangjia.sjdbc.datasource;

import com.fangjia.sjdbc.datasource.config.DataSourceConfig;
import com.fangjia.sjdbc.datasource.config.MultiDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 数据源工具类
 */
public class DataSourceUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceUtil.class);

    private static DataSourceUtil dataSourceUtil;

    public static DataSourceUtil getInstance() {
        if (dataSourceUtil == null) {
            dataSourceUtil = new DataSourceUtil();
        }
        return dataSourceUtil;
    }

    public void changeDataSourceToMater() {
        MultiDataSource.getInstance().switchDataSource(DataSourceConfig.masterDataBaseMessage);
        //logger.debug("当前数据库 ： " + DataSourceContextHolder.getDB());
    }

    public void changeDataSourceToDashboard() {
        MultiDataSource.getInstance().switchDataSource(DataSourceConfig.dashBoardDataBaseMessage);
        //logger.debug("当前数据库 ： " + DataSourceContextHolder.getDB());
    }


    public static void changeDataSource(DataBaseMessage dataBaseMessage) {

        // 切换数据源
        String url = "jdbc:mysql://" + dataBaseMessage.getHost() + ":" + dataBaseMessage.getPort() + "/" + dataBaseMessage.getDbName() + "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull";
        dataBaseMessage.setUrl(url);
        MultiDataSource.getInstance().switchDataSource(dataBaseMessage);
        //logger.debug("当前数据库 ： " + DataSourceContextHolder.getDB());
    }


}