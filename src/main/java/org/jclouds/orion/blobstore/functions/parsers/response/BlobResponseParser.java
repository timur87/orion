/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.orion.blobstore.functions.parsers.response;

/**
 * @author timur
 *
 */

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.HttpResponse;
import org.jclouds.orion.OrionApi;
import org.jclouds.orion.OrionUtils;
import org.jclouds.orion.blobstore.functions.converters.OrionBlobToBlob;
import org.jclouds.orion.domain.BlobType;
import org.jclouds.orion.domain.MutableBlobProperties;
import org.jclouds.orion.domain.OrionBlob;
import org.jclouds.orion.domain.OrionBlob.Factory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class BlobResponseParser implements Function<HttpResponse, Blob> {
	
	private final ObjectMapper mapper;
	private final OrionApi api;
	private final Factory orionBlobProvider;
	private final OrionBlobToBlob orionBlob2Blob;
	private final OrionUtils orionUtils;
	
	@Inject
	public BlobResponseParser(ObjectMapper mapper, OrionApi api, OrionUtils orionUtils,
			OrionBlob.Factory orionBlobProvider, OrionBlobToBlob orionBlob2Blob) {
		this.mapper = Preconditions.checkNotNull(mapper, "mapper is null");
		this.api = Preconditions.checkNotNull(api, "api is null");
		this.orionBlobProvider = Preconditions.checkNotNull(orionBlobProvider, "orionBlobProvider is null");
		this.orionBlob2Blob = Preconditions.checkNotNull(orionBlob2Blob, "orionBlob2Blob is null");
		this.orionUtils = orionUtils;
		
	}
	
	@Override
	public Blob apply(HttpResponse response) {
		
		final StringWriter writer = new StringWriter();
		MutableBlobProperties properties = null;
		try {
			IOUtils.copy(response.getPayload().getInput(), writer);
			final String theString = writer.toString();
			properties = this.mapper.readValue(theString, MutableBlobProperties.class);
			final OrionBlob orionBlob = this.orionBlobProvider.create(properties);
			if (properties.getType() == BlobType.FILE_BLOB) {
				final HttpResponse payloadRes = this.api.getBlobContents(getOrionUtils().getUserWorkspace(), properties.getContainer(),
						properties.getParentPath(), properties.getName());
				orionBlob.setPayload(payloadRes.getPayload());
			}
			return this.orionBlob2Blob.apply(orionBlob);
			
		} catch (final IOException e) {
			System.out.println(response.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}
	
	private OrionUtils getOrionUtils() {
		return this.orionUtils;
	}
	
	
	
}