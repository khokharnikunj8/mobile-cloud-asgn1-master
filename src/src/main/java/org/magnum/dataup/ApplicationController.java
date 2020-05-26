/*
 *
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import retrofit.http.Multipart;
import retrofit.http.Streaming;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
public class ApplicationController {

    /**
     * You will need to create one or more Spring controllers to fulfill the
     * requirements of the assignment. If you use this file, please rename it
     * to something other than "AnEmptyController"
     * <p>
     * ________  ________  ________  ________          ___       ___  ___  ________  ___  __
     * |\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \
     * \ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_
     * \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \
     * \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \
     * \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
     * \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
     */

    private final List<Video> videos = new ArrayList<>();
    VideoFileManager vd = VideoFileManager.get();
    private long id = 1;

    public ApplicationController() throws IOException {
    }

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    @ResponseBody
    public Collection<Video> getVideoList() {
        return videos;
    }

    @RequestMapping(value = "/video", method = RequestMethod.POST)
    @ResponseBody
    public Video addVideo(@RequestBody Video video) {
        video.setId(id++);
        video.setDataUrl(getDataUrl(video.getId()));
        videos.add(video);
        return video;
    }


    private String getDataUrl(long videoId) {
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://" + request.getServerName()
                        + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
        return base;
    }


    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
    @ResponseBody
    @Streaming
    public ResponseEntity getData(@PathVariable(value = "id", required = false) Long id) throws IOException {
        if (id == null || id.longValue() - 1 < 0 || id.longValue() - 1 >= videos.size())
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        Video temp = videos.get((int) id.longValue() - 1);
        if (!vd.hasVideoData(temp)) return new ResponseEntity(HttpStatus.NOT_FOUND);
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        vd.copyVideoData(temp, byteOutputStream);
        return new ResponseEntity(byteOutputStream.toByteArray(), HttpStatus.OK);

    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
    @ResponseBody
    @Multipart
    public ResponseEntity setVideoData(@PathVariable(value = "id", required = false) Long id, @RequestParam(value = "data", required = false) MultipartFile multipartFile) throws IOException {
        if (id == null || id.longValue() - 1 < 0 || id.longValue() - 1 >= videos.size() || multipartFile == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        Video temp = videos.get((int) id.longValue() - 1);

        vd.saveVideoData(temp, multipartFile.getInputStream());
        return new ResponseEntity(new VideoStatus(VideoStatus.VideoState.READY), HttpStatus.OK);
    }


}
