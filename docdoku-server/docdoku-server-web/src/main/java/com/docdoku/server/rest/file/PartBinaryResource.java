/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2014 DocDoku SARL
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
package com.docdoku.server.rest.file;

import com.docdoku.core.common.BinaryResource;
import com.docdoku.core.exceptions.*;
import com.docdoku.core.exceptions.NotAllowedException;
import com.docdoku.core.product.PartIterationKey;
import com.docdoku.core.product.PartRevision;
import com.docdoku.core.security.UserGroupMapping;
import com.docdoku.core.services.IConverterManagerLocal;
import com.docdoku.core.services.IDataManagerLocal;
import com.docdoku.core.services.IProductManagerLocal;
import com.docdoku.core.services.IShareManagerLocal;
import com.docdoku.core.sharing.SharedEntity;
import com.docdoku.core.sharing.SharedPart;
import com.docdoku.server.rest.exceptions.NotModifiedException;
import com.docdoku.server.rest.exceptions.PreconditionFailedException;
import com.docdoku.server.rest.exceptions.RequestedRangeNotSatisfiableException;
import com.docdoku.server.rest.file.util.BinaryResourceDownloadMeta;
import com.docdoku.server.rest.file.util.BinaryResourceDownloadResponseBuilder;
import com.docdoku.server.rest.file.util.BinaryResourceUpload;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
@RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
public class PartBinaryResource{
    @EJB
    private IDataManagerLocal dataManager;
    @EJB
    private IProductManagerLocal productService;
    @EJB
    private IConverterManagerLocal converterService;
    @EJB
    private IShareManagerLocal shareService;

    private static final Logger LOGGER = Logger.getLogger(PartBinaryResource.class.getName());

    public PartBinaryResource() {
    }

    @POST
    @Path("/{iteration}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDirectPartFiles(@Context HttpServletRequest request,
                                          @PathParam("uuid") final String uuid,
                                          @PathParam("workspaceId") final String workspaceId,
                                          @PathParam("partNumber") final String partNumber,
                                          @PathParam("version") final String version,
                                          @PathParam("iteration") final int iteration)
            throws EntityNotFoundException, EntityAlreadyExistsException, UserNotActiveException, AccessRightException, NotAllowedException, CreationException{
        return uploadPartFiles(request,uuid,workspaceId,partNumber,version,iteration,null);
    }

    @POST
    @Path("/{iteration}/{subType}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadPartFiles(@Context HttpServletRequest request,
                                    @PathParam("uuid") final String uuid,
                                    @PathParam("workspaceId") final String workspaceId,
                                    @PathParam("partNumber") final String partNumber,
                                    @PathParam("version") final String version,
                                    @PathParam("iteration") final int iteration,
                                    @PathParam("subType") final String subType)
            throws EntityNotFoundException, EntityAlreadyExistsException, UserNotActiveException, AccessRightException, NotAllowedException, CreationException {
        if(uuid!=null){
            return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                    .build();
        }

        try {
            String fileName=null;
            PartIterationKey partPK = new PartIterationKey(workspaceId, partNumber, version, iteration);
            Collection<Part> formParts = request.getParts();

            for(Part formPart : formParts){
                fileName = uploadAFile(formPart,partPK, subType);
            }

            if(formParts.size()==1) {
                return BinaryResourceUpload.tryToRespondCreated(request.getRequestURI() + fileName);
            }

            return Response.ok().build();

        } catch (IOException | ServletException | StorageException e) {
            return BinaryResourceUpload.uploadError(e);
        }
    }

    private String uploadAFile(Part formPart, PartIterationKey partPK, String subType)
            throws EntityNotFoundException, EntityAlreadyExistsException, UserNotActiveException, CreationException, NotAllowedException, StorageException, IOException {
        BinaryResource binaryResource;
        long length;
        String fileName = formPart.getSubmittedFileName();
        // Init the binary resource with a null length
        if(subType!=null && !subType.isEmpty()){
            binaryResource = productService.saveNativeCADInPartIteration(partPK, fileName, 0);
        }else{
            binaryResource = productService.saveFileInPartIteration(partPK, fileName, 0);
        }
        OutputStream outputStream = dataManager.getBinaryResourceOutputStream(binaryResource);
        length = BinaryResourceUpload.uploadBinary(outputStream, formPart);
        if(subType!=null && !subType.isEmpty()){
            productService.saveNativeCADInPartIteration(partPK, fileName, length);
            tryToConvertCADFileToJSON(partPK,binaryResource);
        }else{
            productService.saveFileInPartIteration(partPK, fileName, length);
        }
        return fileName;
    }

    @GET
    @Path("/{iteration}/{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDirectPartFile(@Context Request request,
                                     @HeaderParam("Range") String range,
                                     @PathParam("uuid") final String uuid,
                                     @PathParam("workspaceId") final String workspaceId,
                                     @PathParam("partNumber") final String partNumber,
                                     @PathParam("version") final String version,
                                     @PathParam("iteration") final int iteration,
                                     @PathParam("fileName") final String fileName,
                                     @QueryParam("type") String type,
                                     @QueryParam("output") String output)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, NotAllowedException, PreconditionFailedException, NotModifiedException, RequestedRangeNotSatisfiableException {
        return downloadPartFile(request,range,uuid,workspaceId,partNumber,version,iteration,null,fileName,type,output);
    }


    @GET
    @Path("/{iteration}/{subType}/{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadPartFile(@Context Request request,
                                     @HeaderParam("Range") String range,
                                     @PathParam("uuid") final String uuid,
                                     @PathParam("workspaceId") final String workspaceId,
                                     @PathParam("partNumber") final String partNumber,
                                     @PathParam("version") final String version,
                                     @PathParam("iteration") final int iteration,
                                     @PathParam("subType") final String subType,
                                     @PathParam("fileName") final String fileName,
                                     @QueryParam("type") String type,
                                     @QueryParam("output") String output)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, NotAllowedException, PreconditionFailedException, NotModifiedException, RequestedRangeNotSatisfiableException {

        String fullName;
        if (uuid != null) {
            SharedEntity sharedEntity = shareService.findSharedEntityForGivenUUID(uuid);
            PartRevision partRevision = ((SharedPart) sharedEntity).getPartRevision();
            fullName = sharedEntity.getWorkspace().getId() +
                    "/parts/" +
                    partRevision.getPartNumber() + "/" +
                    partRevision.getVersion() + "/" +
                    iteration + "/";
        }else {
            // Log guest user                                                                                           // Todo : If Guest, return public binary resource

            // Check access right

            PartIterationKey partIK = new PartIterationKey(workspaceId, partNumber, version,iteration);
            if(!productService.canAccess(partIK)){
                throw new NotAllowedException(Locale.getDefault(),"NotAllowedException34");
            }
            fullName = workspaceId+"/parts/" + partNumber + "/" + version + "/" + iteration + "/";
        }

        fullName += (subType!= null && !subType.isEmpty()) ? subType + "/" +fileName : fileName;

        return downloadPartFile(request,range,fullName,subType,type,output);
    }


    private Response downloadPartFile(Request request, String range, String fullName, String subType, String type, String output)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, NotAllowedException, PreconditionFailedException, NotModifiedException, RequestedRangeNotSatisfiableException {
        BinaryResource binaryResource = productService.getBinaryResource(fullName);
        BinaryResourceDownloadMeta binaryResourceDownloadMeta = new BinaryResourceDownloadMeta(binaryResource,output,type);

        // Check cache precondition
        Response.ResponseBuilder rb = request.evaluatePreconditions(binaryResourceDownloadMeta.getLastModified(), binaryResourceDownloadMeta.getETag());
        if(rb!= null){
            return rb.build();
        }

        if(subType!= null && !subType.isEmpty()){
            binaryResourceDownloadMeta.setSubResourceVirtualPath(subType);
        }

        try {
            InputStream binaryContentInputStream = dataManager.getBinaryResourceInputStream(binaryResource);
            return BinaryResourceDownloadResponseBuilder.prepareResponse(binaryContentInputStream, binaryResourceDownloadMeta, range);
        } catch (StorageException e) {
            return BinaryResourceDownloadResponseBuilder.downloadError(e, fullName);
        }
    }

    private void tryToConvertCADFileToJSON(PartIterationKey partPK, BinaryResource binaryResource){
        try {
            //TODO: Should be put in a DocumentPostUploader plugin
            converterService.convertCADFileToJSON(partPK, binaryResource);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "A CAD file conversion can not be done", e);
        }
    }
}