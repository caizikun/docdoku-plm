/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2017 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.cli.commands.documents;

import com.docdoku.api.client.ApiException;
import com.docdoku.api.models.DocumentIterationDTO;
import com.docdoku.api.models.DocumentRevisionDTO;
import com.docdoku.api.models.utils.LastIterationHelper;
import com.docdoku.api.services.DocumentApi;
import com.docdoku.cli.commands.BaseCommandLine;
import com.docdoku.cli.helpers.FileHelper;
import com.docdoku.cli.helpers.MetaDirectoryManager;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Morgan Guimard
 */
public class DocumentGetCommand extends BaseCommandLine {

    @Option(metaVar = "<revision>", name="-r", aliases = "--revision", usage="specify revision of the document to retrieve ('A', 'B'...); default is the latest")
    private String revision;

    @Option(name="-i", aliases = "--iteration", metaVar = "<iteration>", usage="specify iteration of the document to retrieve ('1','2', '24'...); default is the latest")
    private int iteration;

    @Option(metaVar = "<id>", name = "-o", aliases = "--id", usage = "the id of the document to fetch; if not specified choose the document corresponding to the file")
    private String id;

    @Argument(metaVar = "[<file> | <dir>]", index=0, usage = "specify the file of the document to fetch or the path where files are stored (default is working directory)")
    private File path = new File(System.getProperty("user.dir"));

    @Option(name="-f", aliases = "--force", usage="overwrite existing files even if they have been modified locally")
    private boolean force;

    @Option(name="-w", aliases = "--workspace", required = true, metaVar = "<workspace>", usage="workspace on which operations occur")
    protected String workspace;

    @Override
    public void execImpl() throws Exception {

        if(id==null){
            loadMetadata();
        }

        String strRevision = revision==null?null:revision;
        getDocument(id,strRevision,iteration);

    }

    private void loadMetadata() throws IOException {
        if(path.isDirectory()){
            throw new IllegalArgumentException(langHelper.getLocalizedMessage("DocumentIdOrRevisionNotSpecified1"));
        }
        MetaDirectoryManager meta = new MetaDirectoryManager(path.getParentFile());
        String filePath = path.getAbsolutePath();
        id = meta.getDocumentId(filePath);
        String strRevision = meta.getRevision(filePath);
        if(id==null || strRevision==null){
            throw new IllegalArgumentException(langHelper.getLocalizedMessage("DocumentIdOrRevisionNotSpecified2"));
        }
        revision = strRevision;
        iteration=0;
        path=path.getParentFile();
    }

    private void getDocument(String pId, String pRevision, int pIteration) throws IOException, ApiException, LoginException, NoSuchAlgorithmException {

        DocumentApi documentApi = new DocumentApi(client);
        DocumentRevisionDTO dr = documentApi.getDocumentRevision(workspace,pId,pRevision);

        DocumentIterationDTO di;

        if(pIteration == 0){
            di = LastIterationHelper.getLastIteration(dr);
        }else if(pIteration > dr.getDocumentIterations().size()){
            throw new IllegalArgumentException(langHelper.getLocalizedMessage("IterationNotExisting"));
        }else{
            di = dr.getDocumentIterations().get(pIteration-1);
        }

        if(di.getAttachedFiles().isEmpty()){
            output.printInfo(langHelper.getLocalizedMessage("NoFilesForDocument") + " : "  + id + " " + dr.getVersion() + "." + di.getIteration() + " (" + workspace + ")");
        }else{
            FileHelper fh = new FileHelper(user,password,output,langHelper);
            fh.downloadDocumentFiles(getServerURL(), path, workspace, id, dr, di, force);
        }

    }

    @Override
    public String getDescription() throws IOException {
        return langHelper.getLocalizedMessage("DocumentGetCommandDescription");
    }
}
