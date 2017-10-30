package org.apache.nifi.processors.hive;

import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.exception.ProcessException;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestHiveParser extends AbstractHiveQLProcessor {

    @Override
    public void onTrigger(ProcessContext context, ProcessSessionFactory sessionFactory) throws ProcessException {

    }

    @Test
    public void parseSelect() throws Exception {
        String query = "select a.empid, to_something(b.saraly) from " +
                "company.emp a inner join default.salary b where a.empid = b.empid";
        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(2, tableNames.size());
        assertTrue(tableNames.contains(new TableName("COMPANY", "EMP")));
        assertTrue(tableNames.contains(new TableName("DEFAULT", "SALARY")));
        for (TableName tableName : tableNames) {
            assertTrue(tableName.isInput());
        }
    }

    @Test
    public void parseSelectPrepared() throws Exception {
        String query = "select empid from company.emp a where a.firstName = ?";
        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(1, tableNames.size());
        assertTrue(tableNames.contains(new TableName("COMPANY", "EMP")));
        for (TableName tableName : tableNames) {
            assertTrue(tableName.isInput());
        }
    }


    @Test
    public void parseLongSelect() throws Exception {
        String query = "select\n" +
                "\n" +
                "    i_item_id,\n" +
                "\n" +
                "    i_item_desc,\n" +
                "\n" +
                "    s_state,\n" +
                "\n" +
                "    count(ss_quantity) as store_sales_quantitycount,\n" +
                "\n" +
                "    avg(ss_quantity) as store_sales_quantityave,\n" +
                "\n" +
                "    stddev_samp(ss_quantity) as store_sales_quantitystdev,\n" +
                "\n" +
                "    stddev_samp(ss_quantity) / avg(ss_quantity) as store_sales_quantitycov,\n" +
                "\n" +
                "    count(sr_return_quantity) as store_returns_quantitycount,\n" +
                "\n" +
                "    avg(sr_return_quantity) as store_returns_quantityave,\n" +
                "\n" +
                "    stddev_samp(sr_return_quantity) as store_returns_quantitystdev,\n" +
                "\n" +
                "    stddev_samp(sr_return_quantity) / avg(sr_return_quantity) as store_returns_quantitycov,\n" +
                "\n" +
                "    count(cs_quantity) as catalog_sales_quantitycount,\n" +
                "\n" +
                "    avg(cs_quantity) as catalog_sales_quantityave,\n" +
                "\n" +
                "    stddev_samp(cs_quantity) / avg(cs_quantity) as catalog_sales_quantitystdev,\n" +
                "\n" +
                "    stddev_samp(cs_quantity) / avg(cs_quantity) as catalog_sales_quantitycov\n" +
                "\n" +
                "from\n" +
                "\n" +
                "    store_sales,\n" +
                "\n" +
                "    store_returns,\n" +
                "\n" +
                "    catalog_sales,\n" +
                "\n" +
                "    date_dim d1,\n" +
                "\n" +
                "    date_dim d2,\n" +
                "\n" +
                "    date_dim d3,\n" +
                "\n" +
                "    store,\n" +
                "\n" +
                "    item\n" +
                "\n" +
                "where\n" +
                "\n" +
                "    d1.d_quarter_name = '2000Q1'\n" +
                "\n" +
                "        and d1.d_date_sk = ss_sold_date_sk\n" +
                "\n" +
                "        and i_item_sk = ss_item_sk\n" +
                "\n" +
                "        and s_store_sk = ss_store_sk\n" +
                "\n" +
                "        and ss_customer_sk = sr_customer_sk\n" +
                "\n" +
                "        and ss_item_sk = sr_item_sk\n" +
                "\n" +
                "        and ss_ticket_number = sr_ticket_number\n" +
                "\n" +
                "        and sr_returned_date_sk = d2.d_date_sk\n" +
                "\n" +
                "        and d2.d_quarter_name in ('2000Q1' , '2000Q2', '2000Q3')\n" +
                "\n" +
                "        and sr_customer_sk = cs_bill_customer_sk\n" +
                "\n" +
                "        and sr_item_sk = cs_item_sk\n" +
                "\n" +
                "        and cs_sold_date_sk = d3.d_date_sk\n" +
                "\n" +
                "        and d3.d_quarter_name in ('2000Q1' , '2000Q2', '2000Q3')\n" +
                "\n" +
                "group by i_item_id , i_item_desc , s_state\n" +
                "\n" +
                "order by i_item_id , i_item_desc , s_state\n" +
                "\n" +
                "limit 100";

        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(6, tableNames.size());
        AtomicInteger cnt = new AtomicInteger(0);
        for (TableName tableName : tableNames) {
            if (tableName.equals(new TableName(null, "STORE_SALES"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "STORE_RETURNS"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "CATALOG_SALES"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "DATE_DIM"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "STORE"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "ITEM"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            }
        }
        assertEquals(6, cnt.get());
    }

    @Test
    public void parseInsert() throws Exception {
        String query = "insert into databaseB.tableB1 select something from tableA1 a1 inner join tableA2 a2 where a1.id = a2.id";

        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(3, tableNames.size());
        AtomicInteger cnt = new AtomicInteger(0);
        tableNames.forEach(tableName -> {
            if (tableName.equals(new TableName("DATABASEB", "TABLEB1"))) {
                assertTrue(!tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "TABLEA1"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            } else if (tableName.equals(new TableName(null, "TABLEA2"))) {
                assertTrue(tableName.isInput());
                cnt.incrementAndGet();
            }
        });
        assertEquals(3, cnt.get());
    }

    @Test
    public void parseUpdate() throws Exception {
        String query = "update table_a set y = 'updated' where x > 100";

        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(1, tableNames.size());
        assertTrue(tableNames.contains(new TableName(null, "TABLE_A")));
        assertTrue(!tableNames.iterator().next().isInput());
    }

    @Test
    public void parseDelete() throws Exception {
        String query = "delete from table_a where x > 100";

        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(1, tableNames.size());
        assertTrue(tableNames.contains(new TableName(null, "TABLE_A")));
        assertTrue(!tableNames.iterator().next().isInput());
    }

    @Test
    public void parseDDL() throws Exception {
        String query = "CREATE TABLE IF NOT EXISTS EMPLOYEES(\n" +
                "EmployeeID INT,FirstName STRING, Title STRING,\n" +
                "State STRING, Laptop STRING)\n" +
                "COMMENT 'Employee Names'\n" +
                "STORED AS ORC";


        final Set<TableName> tableNames = findTableNames(query);
        System.out.printf("tableNames=%s\n", tableNames);
        assertEquals(1, tableNames.size());
        assertTrue(tableNames.contains(new TableName(null, "EMPLOYEES")));
        assertTrue(!tableNames.iterator().next().isInput());
    }


}