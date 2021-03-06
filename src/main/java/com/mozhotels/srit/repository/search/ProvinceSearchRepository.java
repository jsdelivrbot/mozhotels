package com.mozhotels.srit.repository.search;

import com.mozhotels.srit.domain.Province;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data ElasticSearch repository for the Province entity.
 */
public interface ProvinceSearchRepository extends ElasticsearchRepository<Province, Long> {
}
