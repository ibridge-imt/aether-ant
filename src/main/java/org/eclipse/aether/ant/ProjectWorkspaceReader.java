/*******************************************************************************
 * Copyright (c) 2010, 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.ant;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.eclipse.aether.ant.types.Pom;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * Workspace reader caching available POMs and artifacts for ant builds.
 * <p/>
 * &lt;pom> elements are cached if they are defined by the 'file'-attribute, as they reference a backing pom.xml file that
 * can be used for resolution with Aether. &lt;artifact> elements are cached if they directly define a 'pom'-attribute
 * or child. The POM may be file-based or in-memory.
 */
public class ProjectWorkspaceReader
    implements WorkspaceReader
{

    private static ProjectWorkspaceReader instance;

    private static Object lock = new Object();

    private Map<String, File> artifacts = Collections.synchronizedMap( new HashMap<String, File>() );

    public void addPom( Pom pom )
    {
        if ( pom.getFile() != null )
        {
            Model model = pom.getModel( pom );
            String coords = coords( new DefaultArtifact( model.getGroupId(), model.getArtifactId(), null, "pom", model.getVersion() ) );
            artifacts.put( coords, pom.getFile() );
        }
    }

    public void addArtifact( org.eclipse.aether.ant.types.Artifact artifact )
    {
        if ( artifact.getPom() != null )
        {
            String coords;

            Pom pom = artifact.getPom();
            DefaultArtifact aetherArtifact;
            if ( pom.getFile() != null )
            {
                Model model = pom.getModel( pom );
                aetherArtifact =
                    new DefaultArtifact( model.getGroupId(), model.getArtifactId(), artifact.getClassifier(),
                                             artifact.getType(),
                                             model.getVersion() );
            }
            else
            {
                aetherArtifact =
                    new DefaultArtifact( pom.getGroupId(), pom.getArtifactId(), artifact.getClassifier(),
                                         artifact.getType(), pom.getVersion() );
            }

            coords = coords( aetherArtifact );
            artifacts.put( coords, artifact.getFile() );
        }
    }

    private String coords( Artifact artifact )
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( artifact.getGroupId() );
        buffer.append( ':' ).append( artifact.getArtifactId() );
        buffer.append( ':' ).append( artifact.getExtension() );
        buffer.append( ':' ).append( artifact.getClassifier() );
        buffer.append( ':' ).append( artifact.getVersion() );
        return buffer.toString();
    }

    public WorkspaceRepository getRepository()
    {
        return new WorkspaceRepository( "ant" );
    }

    public File findArtifact( Artifact artifact )
    {
        return artifacts.get( coords( artifact ) );
    }

    public List<String> findVersions( Artifact artifact )
    {
        return Collections.emptyList();
    }

    ProjectWorkspaceReader()
    {
    }

    public static ProjectWorkspaceReader getInstance()
    {
        if ( instance != null )
        {
            return instance;
        }

        synchronized ( lock )
        {
            if ( instance == null )
            {
                instance = new ProjectWorkspaceReader();
            }
            return instance;
        }
    }

    public static void dropInstance()
    {
        instance = null;
    }
}
