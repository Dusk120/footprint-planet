package com.qzxx.footprint_planet.controller;



import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.NoteDTO;
import com.qzxx.footprint_planet.dto.request.NoteLikeDTO;
import com.qzxx.footprint_planet.dto.request.NoteQueryDTO;
import com.qzxx.footprint_planet.service.NoteService;
import com.qzxx.footprint_planet.vo.NoteLikeVO;
import com.qzxx.footprint_planet.vo.NoteVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    @Resource
    private NoteService noteService;

    @GetMapping
    public Result<PageVO<NoteVO>> getNotes(@Valid NoteQueryDTO noteQueryDTO) {
        return Result.success(noteService.getNotes(noteQueryDTO));
    }

    @GetMapping("/{id}")
    public Result<NoteVO> getNote(@PathVariable Long id) {
        return Result.success(noteService.getNote(id));
    }

    @PostMapping
    public Result<NoteVO> addNote(@RequestBody @Valid NoteDTO noteDTO) {
        return Result.success(noteService.addNote(noteDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    public Result<NoteLikeVO> likeNote(@PathVariable Long id, @RequestBody @Valid NoteLikeDTO noteLikeDTO) {
        return Result.success(noteService.likeNote(id, noteLikeDTO));
    }
}
