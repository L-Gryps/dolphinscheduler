/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.dao.plugin.dm.monitor;

import org.apache.dolphinscheduler.dao.plugin.api.monitor.DatabaseMetrics;
import org.apache.dolphinscheduler.dao.plugin.api.monitor.DatabaseMonitor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import javax.sql.DataSource;

import lombok.SneakyThrows;

import com.baomidou.mybatisplus.annotation.DbType;

public class DamengMonitor implements DatabaseMonitor {

    private final DataSource dataSource;

    public DamengMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @SneakyThrows
    @Override
    public DatabaseMetrics getDatabaseMetrics() {
        DatabaseMetrics monitorRecord = new DatabaseMetrics();
        monitorRecord.setDate(new Date());
        monitorRecord.setDbType(DbType.DM);
        monitorRecord.setState(DatabaseMetrics.DatabaseHealthStatus.YES);

        try (
                Connection connection = dataSource.getConnection();
                Statement stmt = connection.createStatement()) {
            try (
                    ResultSet rs = stmt.executeQuery(
                            "select default_value from v$parameter where name = 'MAX_SESSIONS'")) {
                if (rs.next()) {
                    monitorRecord.setMaxConnections(Long.parseLong(rs.getString("default_value")));
                }
            }

            try (
                    ResultSet rs = stmt.executeQuery(
                            "select count(*) as cnt from v$sessions")) {
                if (rs.next()) {
                    monitorRecord.setThreadsConnections(rs.getLong("cnt"));
                }
            }

            try (
                    ResultSet rs = stmt.executeQuery(
                            "select count(*) as cnt from v$sessions where state = 'ACTIVE'")) {
                if (rs.next()) {
                    monitorRecord.setThreadsRunningConnections(rs.getLong("cnt"));
                }
            }
            if (monitorRecord.getMaxUsedConnections() == 0) {
                monitorRecord.setMaxUsedConnections(monitorRecord.getThreadsConnections());
            }

        } catch (Exception e) {
            monitorRecord.setState(DatabaseMetrics.DatabaseHealthStatus.NO);
            throw new RuntimeException("Get Dameng database metrics failed", e);
        }
        return monitorRecord;
    }
}
