package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.NoteDTO;
import com.qzxx.footprint_planet.dto.request.NoteLikeDTO;
import com.qzxx.footprint_planet.dto.request.NoteQueryDTO;
import com.qzxx.footprint_planet.vo.NoteLikeVO;
import com.qzxx.footprint_planet.vo.NoteVO;
import com.qzxx.footprint_planet.vo.PageVO;

/** 社区笔记（列表 / 发布 / 删除 / 点赞） */
public interface NoteService {
    PageVO<NoteVO> getNotes(NoteQueryDTO noteQueryDTO);

    NoteVO getNote(Long id);

    NoteVO addNote( NoteDTO noteDTO);

    void deleteNote(Long id);

    NoteLikeVO likeNote(Long id, NoteLikeDTO noteLikeDTO);
}
