package org.apache.velocity.runtime.resource.loader;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.Map;
import java.util.Hashtable;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.InitialContext;

import org.apache.velocity.runtime.Runtime;
import org.apache.velocity.runtime.resource.Resource;

/**
 * This is a simple template file loader that loads templates
 * from a DataSource instead of plain files.
 * 
 * It can be configured with a datasource name, a table name,
 * id column (name), content column (the template body) and a
 * timestamp column (for last modification info).
 * 
 * Example configuration snippet for velociy.properties:
 *
 * resource.loader.1.public.name = DB 
 * resource.loader.1.description = Velocity DB Resource Loader 
 * resource.loader.1.class = org.apache.velocity.runtime.resource.loader.DBResourceLoader 
 * resource.loader.1.resource.datasource = jdbc/SomeDS 
 * resource.loader.1.resource.table = template_table 
 * resource.loader.1.resource.keycolumn = template_id 
 * resource.loader.1.resource.templatecolumn = template_definition 
 * resource.loader.1.resource.timestampcolumn = template_timestamp 
 * resource.loader.1.cache = false 
 * resource.loader.1.modificationCheckInterval = 60
 *
 * @author <a href="mailto:david.kinnvall@alertir.com">David Kinnvall</a>
 */

public class DataSourceResourceLoader extends ResourceLoader
{
     private String dataSourceName;
     private String tableName;
     private String keyColumn;
     private String templateColumn;
     private String timestampColumn;
     private InitialContext ctx;
     private DataSource dataSource;

     /*
      * This should probably be moved into the super class,
      * the stand init stuff. For the properties that all
      * loaders will probably share.
      */
     public void init(Map initializer)
     {
         dataSourceName  = (String) initializer.get("resource.datasource");
         tableName       = (String) initializer.get("resource.table");
         keyColumn       = (String) initializer.get("resource.keycolumn");
         templateColumn  = (String) initializer.get("resource.templatecolumn");
         timestampColumn = (String) initializer.get("resource.timestampcolumn");
         Runtime.info("Resources Loaded From: " + dataSourceName + "/" + tableName);
         Runtime.info( "Resource Loader using columns: " + keyColumn + ", "
                       + templateColumn + " and " + timestampColumn);
         Runtime.info("Resource Loader Initalized.");
     }


     public boolean isSourceModified(Resource resource)
     {
         return (resource.getLastModified() != 
                 readLastModified(resource, "checking timestamp"));
     }

     public long getLastModified(Resource resource)
     {
         return readLastModified(resource, "getting timestamp");
     }

     /**
      * Get an InputStream so that the Runtime can build a
      * template with it.
      */
     public synchronized InputStream getResourceStream( String name )
         throws Exception
     {
         if (name == null || name.length() == 0)
         {
             throw new Exception ("Need to specify a template name!");
         }

         try
         {   
             Connection conn = openDbConnection();
             
             try
             {   
                 ResultSet rs = readData(conn, templateColumn, name);
                 
                 try
                 {   
                     if (rs.next())
                     {
                         return new
                             BufferedInputStream(rs.getAsciiStream(templateColumn));
                     }
                     else
                     {
                         Runtime.error(
                            "DBResourceLoader Error: cannot find resource " 
                            + name);
                     }
                 }
                 finally
                 {   
                     rs.close();
                 }
             }
             finally
             {   
                 closeDbConnection(conn);
             }
         }
         catch(Exception e)
         {   
             Runtime.error( 
                  "DBResourceLoader Error: database problem trying to load resource "
                     + name + ": " + e.toString() );
         }
         return null;
     }


     private long readLastModified(Resource resource, String i_operation)
     {
         String name = resource.getName();
         try
         {   
             Connection conn = openDbConnection();
             
             try
             {   
                 ResultSet rs = readData(conn, timestampColumn, name);
                 try
                 {   
                     if (rs.next())
                     {
                         return rs.getLong(timestampColumn);
                     }
                     else
                     {
                         Runtime.error("DBResourceLoader Error: while " 
                                       + i_operation 
                                       + " could not find resource " + name);
                     }
                 }
                 finally
                 {   
                     rs.close();
                 }
             }
             finally
             {   
                 closeDbConnection(conn);
             }
         }
         catch(Exception e)
         {   
             Runtime.error( "DBResourceLoader Error: error while " 
                 + i_operation + " when trying to load resource "
                 + name + ": " + e.toString() );
         }
         return 0;
     }

     private Connection openDbConnection()
         throws Exception
    {
         if(ctx == null)
         {
             ctx = new InitialContext();
         }
         
         if(dataSource == null)
         {
             dataSource = (DataSource)ctx.lookup(dataSourceName);
         }

         return dataSource.getConnection();
     }

     private void closeDbConnection(Connection conn)
     {  
         try 
         {
             conn.close();
         } 
         catch (Exception e) 
         {
             Runtime.info(
                 "DBResourceLoader Quirk: problem when closing connection: " 
                 + e.toString());
         }
     }

     private ResultSet readData(Connection conn, String columnNames, String templateName)
         throws SQLException
     {   
         Statement stmt = conn.createStatement();
         
         String sql = "select " + columnNames
                      + " from " + tableName
                      + " where " + keyColumn + " = '" + templateName + "'";

         return stmt.executeQuery(sql);
     }
}




