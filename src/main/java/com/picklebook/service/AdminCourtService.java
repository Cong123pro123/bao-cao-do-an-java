package com.picklebook.service;

import com.picklebook.dto.request.CourtForm;
import com.picklebook.entity.Court;
import com.picklebook.entity.CourtImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminCourtService {

    List<Court> getAllCourts();

    Court getCourt(long id);

    Court create(CourtForm form);

    Court update(long id, CourtForm form);

    List<CourtImage> getCourtImages(long courtId);

    CourtImage addCourtImage(long courtId, MultipartFile file, String imageUrl, String altText, boolean primary);

    void deleteCourtImage(long courtId, long imageId);

    void setPrimaryCourtImage(long courtId, long imageId);
}
