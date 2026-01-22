package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class XtreamCategory {
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_name")
    private String categoryName;
    @JsonProperty("parent_id")
    private int parentId;
}
