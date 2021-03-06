/*******************************************************************************
* Copyright (c) 2015 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package com.mycorp.examples.student.remoteservice.host;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.osgi.service.component.annotations.Component;

import com.mycorp.examples.student.Address;
import com.mycorp.examples.student.Student;
import com.mycorp.examples.student.StudentService;
import com.mycorp.examples.student.Students;

// The jax-rs path annotation for this service
@Path("/studentservice")
// The OSGi DS (declarative services) component annotation. 
@Component(immediate = true, property = { "service.exported.interfaces=*", 
		"service.exported.intents=osgi.async",
		"service.exported.intents=jaxrs","osgi.basic.timeout=5000000",
		"ecf.jaxrs.jersey.server.pathPrefix=/osbee",
		"ecf.jaxrs.jersey.server.alias=/jose"})
public class StudentServiceImpl implements StudentService {

	// Provide a map-based storage of students
	private static Map<String, Student> students = Collections.synchronizedMap(new HashMap<String, Student>());
	// Create a single student and add to students map
	static {
		Student s = new Student("Joe Senior");
		s.setId(UUID.randomUUID().toString());
		s.setGrade("First");
		Address a = new Address();
		a.setCity("New York");
		a.setState("NY");
		a.setPostalCode("11111");
		a.setStreet("111 Park Ave");
		s.setAddress(a);
		students.put(s.getId(), s);
	}

	// Implementation of StudentService based upon the students map
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/students")
	public Students getStudents() {
		Students result = new Students();
		result.setStudents(new ArrayList<Student>(students.values()));
		return result;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/studentscf")
	public CompletableFuture<Students> getStudentsCF() {
		CompletableFuture<Students> cf = new CompletableFuture<Students>();
		cf.complete(getStudents());
		return cf;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/students/{studentId}")
	public Student getStudent(@PathParam("studentId") String id) {
		return students.get(id);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/students/{studentName}")
	public Student createStudent(@PathParam("studentName") String studentName) {
		if (studentName == null)
			return null;
		synchronized (students) {
			Student s = new Student(studentName);
			s.setId(UUID.randomUUID().toString());
			students.put(s.getId(), s);
			return s;
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/students")
	public Student updateStudent(Student student) {
		Student result = null;
		if (student != null) {
			String id = student.getId();
			if (id != null) {
				synchronized (students) {
					result = students.get(student.getId());
					if (result != null) {
						String newName = student.getName();
						if (newName != null)
							result.setName(newName);
						result.setGrade(student.getGrade());
						result.setAddress(student.getAddress());
					}
				}
			}
		}
		return result;
	}

	@DELETE
	@Path("/students/{studentId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Student deleteStudent(@PathParam("studentId") String studentId) {
		return students.remove(studentId);
	}
	
	@POST
	@Path("/upload/{student}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadSomething(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail
			,@PathParam("student")  String studentname
			) throws Exception  {

	    String UPLOAD_PATH = "c:/temp/"+studentname;
	    try
	    {
	        int read = 0;
	        byte[] bytes = new byte[1024];
	 
	        Student sFound = null;
	        for( Student s:students.values() ) {
	        	if ( s.getName().equals(studentname) ) {
	        		sFound = s;
	        		break;
	        	}
	        }
	        
	        if ( sFound == null )
	        	throw new WebApplicationException("Student not found. Please try again !!");
	        
		    File target = new File(UPLOAD_PATH);
		    target.mkdirs();
	        OutputStream out = new FileOutputStream(new File(target,fileDetail.getFileName().replaceAll(":", "_")));
	        while ((read = uploadedInputStream.read(bytes)) != -1)
	        {
	            out.write(bytes, 0, read);
	        }
	        out.flush();
	        out.close();
	    } catch (IOException e)
	    {
	        throw new WebApplicationException("Error while uploading file. Please try again !!");
	    }
	    return Response.ok("Data uploaded successfully !!").build();

	}
	
	
}
