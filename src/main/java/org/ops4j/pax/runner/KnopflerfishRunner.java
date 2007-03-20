/*
 * Copyright 2006 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.runner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import org.ops4j.pax.runner.pom.BundleManager;
import org.xml.sax.SAXException;

public class KnopflerfishRunner
    implements Runnable
{

    private CmdLine m_cmdLine;
    private Properties m_props;
    private File m_systemBundle;
    private List<File> m_bundles;
    private static final String FRAMEWORK_GROUPID = "org.knopflerfish.osgi";
    private static final String BUNDLES_GROUPID = "org.knopflerfish.bundle";
    private static final String[] SYSTEM_BUNDLE = { FRAMEWORK_GROUPID, "framework", "2.0.0" };

    private static final String[][] DEFAULT_BUNDLES =
        {
            { BUNDLES_GROUPID + ".useradmin", "useradmin_api", "1.1.0" },
            { BUNDLES_GROUPID + ".consoletty", "consoletty", "1.0.1" },
            { BUNDLES_GROUPID + ".frameworkcommands", "frameworkcommands", "1.0.0" },
            { BUNDLES_GROUPID + ".console", "console", "1.0.0" },
            { BUNDLES_GROUPID + ".console", "console_api", "1.0.0" },
            { BUNDLES_GROUPID + ".logcommands", "logcommands", "1.0.1" },
            { "org.ops4j.pax.logging", "api", "0.9.2" },
            { "org.ops4j.pax.logging", "service", "0.9.2" },
            { BUNDLES_GROUPID + ".cm", "cm_api", "1.0.1" }
        };
    private static final String[][] GUI_BUNDLES =
        {
            { BUNDLES_GROUPID + ".desktop", "desktop_all", "1.2.0" },
            { BUNDLES_GROUPID + ".util", "util", "1.0.0" },
            { BUNDLES_GROUPID + ".kxml", "kxml", "1.0" },
            { BUNDLES_GROUPID + ".metatype", "metatype", "1.0.0" },
            { BUNDLES_GROUPID + ".kf_metatype", "kf_metatype", "1.0.0" },
            { BUNDLES_GROUPID + ".cm_desktop", "cm_desktop", "1.0.0" }
        };

    public KnopflerfishRunner( CmdLine cmdLine, Properties props, List<File> bundles, BundleManager bundleManager )
        throws IOException, ParserConfigurationException, SAXException
    {
        m_cmdLine = cmdLine;
        m_props = props;
        m_bundles = bundles;
        m_systemBundle = bundleManager.getBundle( SYSTEM_BUNDLE[ 0 ], SYSTEM_BUNDLE[ 1 ], SYSTEM_BUNDLE[ 2 ] );
        if( m_cmdLine.isSet( "gui" ) )
        {
            for( String[] bundle : GUI_BUNDLES )
            {
                File gui = bundleManager.getBundle( bundle[ 0 ], bundle[ 1 ], bundle[ 2 ] );
                m_bundles.add( 0, gui );
            }
        }
        for( int i = 0; i < DEFAULT_BUNDLES.length; i++ )
        {
            String[] bundle = DEFAULT_BUNDLES[ i ];
            File file = bundleManager.getBundle( bundle[ 0 ], bundle[ 1 ], bundle[ 2 ] );
            m_bundles.add( 0, file );
        }
    }

    public void run()
    {
        try
        {
            File f = createPackageListFile();
            createConfigFile( f );
            runIt();
        } catch( IOException e )
        {
            e.printStackTrace();
        } catch( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    private void createConfigFile( File packageFile )
        throws IOException
    {
        File confDir = Run.WORK_DIR;
        File file = new File( confDir, "init.xargs" );
        if( file.exists() )
        {
            return;
        }
        Writer out = FileUtils.openPropertyFile( file );
        try
        {
            out.write( "#\n# Generated by Pax Runner from http://www.ops4j.org\n#\n" );
            out.write( "#\n" );
            out.write( "# Properties used by both init.xargs and restart.xargs\n" );
            out.write( "#\n" );
            out.write( "\n" );
            out.write( "# The Service Platform ID should be used by bundles needing to\n" );
            out.write( "# a unique ID for the platform itself\n" );
            out.write( "-Dorg.osgi.provisioning.spid=knopflerfish\n" );
            out.write( "-Dorg.osgi.framework.system.packages.file=" );
            out.write( packageFile.getAbsolutePath() );
            out.write( "\n" );
            out.write( "\n" );
            out.write( "# Initial startup verbosity, 0 is low verbosity\n" );
            out.write( "-Dorg.knopflerfish.verbosity=0\n" );
            out.write( "\n" );
            out.write( "# Security\n" );
            out.write( "#-Djava.security.manager=\n" );
            out.write( "#-Djava.security.policy=file:framework.policy\n" );
            out.write( "\n" );
            out.write( "# URL to bundle repository\n" );
            out.write( "-Doscar.repository.url=http://www.knopflerfish.org/repo/repository.xml\n" );
            out.write( "\n" );
            out.write( "# Various debug flags\n" );
            out.write( "-Dorg.knopflerfish.framework.debug.packages=false\n" );
            out.write( "#-Dorg.knopflerfish.framework.debug.packages=true\n" );
            out.write( "-Dorg.knopflerfish.framework.debug.errors=true\n" );
            out.write( "-Dorg.knopflerfish.framework.debug.classloader=false\n" );
            out.write( "#-Dorg.knopflerfish.framework.debug.classloader=true\n" );
            out.write( "-Dorg.knopflerfish.framework.debug.startlevel=false\n" );
            out.write( "-Dorg.knopflerfish.framework.debug.ldap=false\n" );
            out.write( "#-Dorg.knopflerfish.framework.bundlestorage.file.always_unpack=true\n" );
            out.write( "\n" );
            out.write( "# Comma-separated list of packges exported by system classloader\n" );
            out.write( "-Dorg.osgi.framework.system.packages=\n" );
            out.write( "\n" );
            out.write( "# Web server properties\n" );
            out.write( "-Dorg.knopflerfish.http.dnslookup=false\n" );
            out.write( "-Dorg.osgi.service.http.port=8080\n" );
            out.write( "\n" );
            out.write( "-Dorg.knopflerfish.startlevel.use=true\n" );
            out.write( "\n" );
            out.write( "# Log service properties\n" );
            out.write( "-Dorg.knopflerfish.log.out=false\n" );
            out.write( "-Dorg.knopflerfish.log.level=info\n" );
            out.write( "-Dorg.knopflerfish.log.grabio=true\n" );
            out.write( "-Dorg.knopflerfish.log.file=true\n" );
            out.write( "\n" );
            out.write( "#consoletelnet properties\n" );
            out.write( "-Dorg.knopflerfish.consoletelnet.user=admin\n" );
            out.write( "-Dorg.knopflerfish.consoletelnet.pwd=admin\n" );
            out.write( "-Dorg.knopflerfish.consoletelnet.port=8023\n" );

            for( Map.Entry entry : m_props.entrySet() )
            {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                out.write( "-D" );
                out.write( key );
                out.write( "=" );
                out.write( value );
                out.write( "\n" );
            }
            out.write( "-Dorg.knopflerfish.gosg.jars=file://" + confDir.getAbsolutePath() + "/lib/\n\n" );
            out.write( "-init\n" );
            out.write( "-initlevel 1\n" );
            for( File bundle : m_bundles )
            {
                out.write( "-install " );
                out.write( bundle.getAbsolutePath() );
                out.write( "\n" );
            }
            out.write( "-startlevel 7\n" );
            for( File bundle : m_bundles )
            {
                out.write( "-start " );
                out.write( bundle.getAbsolutePath() );
                out.write( "\n" );
            }
            out.flush();
        } finally
        {
            out.close();
        }
    }

    private void runIt()
        throws IOException, InterruptedException
    {
        Runtime runtime = Runtime.getRuntime();
        String[] frameworkOpts = {};
        String frameworkOptsString = System.getProperty( "FRAMEWORK_OPTS" );
        if( frameworkOptsString != null )
        {
            //get framework opts
            frameworkOpts = frameworkOptsString.split( " " );
        }
        String javaHome = System.getProperty( "JAVA_HOME" );
        if( javaHome == null )
        {
            javaHome = System.getenv().get( "JAVA_HOME" );
        }
        if( javaHome == null )
        {
            System.err.println( "JAVA_HOME is not set." );
        }
        else
        {
            String[] commands =
                {
                    "-Dorg.knopflerfish.framework.usingwrapperscript=false",
                    "-Dorg.knopflerfish.framework.exitonshutdown=true",
                    "-jar",
                    m_systemBundle.getAbsolutePath()
                };
          //copy these two together
            String[] totalCommandLine = new String[commands.length + frameworkOpts.length + 1];
            totalCommandLine[0] = javaHome + "/bin/java";
            int i = 0;
            for( i = 0;i < frameworkOpts.length; i++ )
            {
                totalCommandLine[1 + i] = frameworkOpts[i];
            }
            System.arraycopy( commands, 0, totalCommandLine, i + 1, commands.length );
            Process process = runtime.exec( totalCommandLine, null, Run.WORK_DIR );
            InputStream err = process.getErrorStream();
            InputStream out = process.getInputStream();
            OutputStream in = process.getOutputStream();
            Pipe errPipe = new Pipe( err, System.err );
            errPipe.start();
            Pipe outPipe = new Pipe( out, System.out );
            outPipe.start();
            Pipe inPipe = new Pipe( System.in, in );
            inPipe.start();
            process.waitFor();
            inPipe.stop();
            outPipe.stop();
            errPipe.stop();
        }
    }

    private File createPackageListFile()
        throws IOException
    {
        String javaVersion = System.getProperty( "java.version" );
        javaVersion = javaVersion.substring( 0, 3 );
        ClassLoader cl = getClass().getClassLoader();
        String resource = "org/knopflerfish/packages" + javaVersion + ".txt";
        InputStream in = cl.getResourceAsStream( resource );
        if( in == null )
        {
            throw new IllegalStateException( "Resource not found in jar: " + resource );
        }
        File f = new File( Run.WORK_DIR, "system-packages.list" );
        FileOutputStream out = new FileOutputStream( f );
        try
        {
            Downloader.copyStream( in, out, false );
        } finally
        {
            out.close();
        }
        return f;
    }
}
