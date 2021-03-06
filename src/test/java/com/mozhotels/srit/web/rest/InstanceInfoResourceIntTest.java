package com.mozhotels.srit.web.rest;

import com.mozhotels.srit.MozhotelsApp;
import com.mozhotels.srit.domain.InstanceInfo;
import com.mozhotels.srit.repository.InstanceInfoRepository;
import com.mozhotels.srit.repository.search.InstanceInfoSearchRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for the InstanceInfoResource REST controller.
 *
 * @see InstanceInfoResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MozhotelsApp.class)
@WebAppConfiguration
@IntegrationTest
public class InstanceInfoResourceIntTest {

    private static final String DEFAULT_INSTANCE_INFO_NAME = "AAAAA";
    private static final String UPDATED_INSTANCE_INFO_NAME = "BBBBB";
    private static final String DEFAULT_DESCRIPTION = "AAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBB";

    @Inject
    private InstanceInfoRepository instanceInfoRepository;

    @Inject
    private InstanceInfoSearchRepository instanceInfoSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restInstanceInfoMockMvc;

    private InstanceInfo instanceInfo;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        InstanceInfoResource instanceInfoResource = new InstanceInfoResource();
        ReflectionTestUtils.setField(instanceInfoResource, "instanceInfoSearchRepository", instanceInfoSearchRepository);
        ReflectionTestUtils.setField(instanceInfoResource, "instanceInfoRepository", instanceInfoRepository);
        this.restInstanceInfoMockMvc = MockMvcBuilders.standaloneSetup(instanceInfoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        instanceInfoSearchRepository.deleteAll();
        instanceInfo = new InstanceInfo();
        instanceInfo.setInstanceInfoName(DEFAULT_INSTANCE_INFO_NAME);
        instanceInfo.setDescription(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createInstanceInfo() throws Exception {
        int databaseSizeBeforeCreate = instanceInfoRepository.findAll().size();

        // Create the InstanceInfo

        restInstanceInfoMockMvc.perform(post("/api/instance-infos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(instanceInfo)))
                .andExpect(status().isCreated());

        // Validate the InstanceInfo in the database
        List<InstanceInfo> instanceInfos = instanceInfoRepository.findAll();
        assertThat(instanceInfos).hasSize(databaseSizeBeforeCreate + 1);
        InstanceInfo testInstanceInfo = instanceInfos.get(instanceInfos.size() - 1);
        assertThat(testInstanceInfo.getInstanceInfoName()).isEqualTo(DEFAULT_INSTANCE_INFO_NAME);
        assertThat(testInstanceInfo.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);

        // Validate the InstanceInfo in ElasticSearch
        InstanceInfo instanceInfoEs = instanceInfoSearchRepository.findOne(testInstanceInfo.getId());
        assertThat(instanceInfoEs).isEqualToComparingFieldByField(testInstanceInfo);
    }

    @Test
    @Transactional
    public void checkInstanceInfoNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = instanceInfoRepository.findAll().size();
        // set the field null
        instanceInfo.setInstanceInfoName(null);

        // Create the InstanceInfo, which fails.

        restInstanceInfoMockMvc.perform(post("/api/instance-infos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(instanceInfo)))
                .andExpect(status().isBadRequest());

        List<InstanceInfo> instanceInfos = instanceInfoRepository.findAll();
        assertThat(instanceInfos).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllInstanceInfos() throws Exception {
        // Initialize the database
        instanceInfoRepository.saveAndFlush(instanceInfo);

        // Get all the instanceInfos
        restInstanceInfoMockMvc.perform(get("/api/instance-infos?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(instanceInfo.getId().intValue())))
                .andExpect(jsonPath("$.[*].instanceInfoName").value(hasItem(DEFAULT_INSTANCE_INFO_NAME.toString())))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())));
    }

    @Test
    @Transactional
    public void getInstanceInfo() throws Exception {
        // Initialize the database
        instanceInfoRepository.saveAndFlush(instanceInfo);

        // Get the instanceInfo
        restInstanceInfoMockMvc.perform(get("/api/instance-infos/{id}", instanceInfo.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(instanceInfo.getId().intValue()))
            .andExpect(jsonPath("$.instanceInfoName").value(DEFAULT_INSTANCE_INFO_NAME.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingInstanceInfo() throws Exception {
        // Get the instanceInfo
        restInstanceInfoMockMvc.perform(get("/api/instance-infos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateInstanceInfo() throws Exception {
        // Initialize the database
        instanceInfoRepository.saveAndFlush(instanceInfo);
        instanceInfoSearchRepository.save(instanceInfo);
        int databaseSizeBeforeUpdate = instanceInfoRepository.findAll().size();

        // Update the instanceInfo
        InstanceInfo updatedInstanceInfo = new InstanceInfo();
        updatedInstanceInfo.setId(instanceInfo.getId());
        updatedInstanceInfo.setInstanceInfoName(UPDATED_INSTANCE_INFO_NAME);
        updatedInstanceInfo.setDescription(UPDATED_DESCRIPTION);

        restInstanceInfoMockMvc.perform(put("/api/instance-infos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedInstanceInfo)))
                .andExpect(status().isOk());

        // Validate the InstanceInfo in the database
        List<InstanceInfo> instanceInfos = instanceInfoRepository.findAll();
        assertThat(instanceInfos).hasSize(databaseSizeBeforeUpdate);
        InstanceInfo testInstanceInfo = instanceInfos.get(instanceInfos.size() - 1);
        assertThat(testInstanceInfo.getInstanceInfoName()).isEqualTo(UPDATED_INSTANCE_INFO_NAME);
        assertThat(testInstanceInfo.getDescription()).isEqualTo(UPDATED_DESCRIPTION);

        // Validate the InstanceInfo in ElasticSearch
        InstanceInfo instanceInfoEs = instanceInfoSearchRepository.findOne(testInstanceInfo.getId());
        assertThat(instanceInfoEs).isEqualToComparingFieldByField(testInstanceInfo);
    }

    @Test
    @Transactional
    public void deleteInstanceInfo() throws Exception {
        // Initialize the database
        instanceInfoRepository.saveAndFlush(instanceInfo);
        instanceInfoSearchRepository.save(instanceInfo);
        int databaseSizeBeforeDelete = instanceInfoRepository.findAll().size();

        // Get the instanceInfo
        restInstanceInfoMockMvc.perform(delete("/api/instance-infos/{id}", instanceInfo.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean instanceInfoExistsInEs = instanceInfoSearchRepository.exists(instanceInfo.getId());
        assertThat(instanceInfoExistsInEs).isFalse();

        // Validate the database is empty
        List<InstanceInfo> instanceInfos = instanceInfoRepository.findAll();
        assertThat(instanceInfos).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchInstanceInfo() throws Exception {
        // Initialize the database
        instanceInfoRepository.saveAndFlush(instanceInfo);
        instanceInfoSearchRepository.save(instanceInfo);

        // Search the instanceInfo
        restInstanceInfoMockMvc.perform(get("/api/_search/instance-infos?query=id:" + instanceInfo.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(instanceInfo.getId().intValue())))
            .andExpect(jsonPath("$.[*].instanceInfoName").value(hasItem(DEFAULT_INSTANCE_INFO_NAME.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())));
    }
}
