package edu.hawaii.its.api.controller;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import edu.hawaii.its.api.service.HttpRequestService;
import edu.hawaii.its.groupings.configuration.Realm;
import edu.hawaii.its.groupings.configuration.SpringBootWebApplication;
import edu.hawaii.its.groupings.controller.WithMockUhUser;
import edu.hawaii.its.groupings.exceptions.ApiServerHandshakeException;
import edu.hawaii.its.groupings.util.JsonUtil;

@ActiveProfiles("localTest")
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class GroupingsRestControllerTest {

    private static final String GROUPING = "grouping1";
    private static final String GROUPING2 = "grouping2";
    private static final String UID = "user";
    private static final String REST_CONTROLLER_BASE = "/api/groupings/";
    private static final String ADMIN_UID = "admin";

    @Value("${url.api.2.1.base}")
    private String API_2_1_BASE;

    @MockBean
    private HttpRequestService httpRequestService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    GroupingsRestController groupingsRestController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        when(httpRequestService.makeApiRequest(anyString(), anyString(), any(HttpMethod.class)))
                .thenReturn(new ResponseEntity(HttpStatus.BAD_REQUEST));
    }

    @Test
    @WithMockUhUser
    public void rootTest() throws Exception {
        assertNotNull(mockMvc.perform(get(REST_CONTROLLER_BASE).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());
    }

    @Test
    @WithMockUhUser
    public void helloTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "/";
        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());
    }

    @Test
    @WithMockUhUser
    public void currentUidTest() throws Exception {
        String uri = "/currentUser";
        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("{'uid':" + UID + "}"))
                .andReturn());
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void groupingAdminsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "groupings/admins";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void allGroupingsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "groupings";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void hasAdminPrivsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members/" + ADMIN_UID + "/is-admin";

        given(httpRequestService.makeApiRequest(anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void addAdminTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "newAdmin/addAdmin";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.POST)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.POST));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void removeAdminTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "newAdmin/removeAdmin";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void removeFromGroupsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING2 + "/user/removeFromGroups";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void resetIncludeGroupTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/resetIncludeGroup";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void resetIncludeGroupAsyncTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/resetIncludeGroupAsync";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void resetExcludeGroupTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/resetExcludeGroup";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void resetExcludeGroupAsyncTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/resetExcludeGroupAsync";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void resetGroupTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/user1/user2" + "/resetGroup";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser
    public void memberAttributeResultsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members";
        List<String> members = new ArrayList<>();
        members.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.POST)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(members)))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.POST));
    }

    @Test
    @WithMockUhUser
    public void memberAttributeResultsAsyncTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members/membersAsync";
        List<String> members = new ArrayList<>();
        members.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.POST)))
                .willReturn(new ResponseEntity(HttpStatus.ACCEPTED));

        assertNotNull(mockMvc.perform(post(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(members)))
                .andExpect(status().isAccepted())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.POST));
    }

    @Test
    @WithMockUhUser
    public void membershipResultsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members/memberships";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void manageSubjectResultsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members/0000/groupings";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void getNumberOfMembershipsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members/memberships/count";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser(uid = "admin")
    public void optInGroupsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "groupings/optInGroups";

        given(httpRequestService.makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(ADMIN_UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void optInTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/optIn";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void optOutTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/optOut";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void addIncludeMembersTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/addIncludeMembers";
        List<String> usersToAdd = new ArrayList<>();
        usersToAdd.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(put(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(usersToAdd)))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT));

    }

    @Test
    @WithMockUhUser
    public void addIncludeMembersAsyncTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/addIncludeMembersAsync";
        List<String> usersToAdd = new ArrayList<>();
        usersToAdd.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.ACCEPTED));

        assertNotNull(mockMvc.perform(put(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(usersToAdd)))
                .andExpect(status().isAccepted())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT));

    }

    @Test
    @WithMockUhUser
    public void addExcludeMembersTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/addExcludeMembers";
        List<String> usersToAdd = new ArrayList<>();
        usersToAdd.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(put(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(usersToAdd)))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void addExcludeMembersAsyncTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/addExcludeMembersAsync";
        List<String> usersToAdd = new ArrayList<>();
        usersToAdd.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.ACCEPTED));

        assertNotNull(mockMvc.perform(put(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(usersToAdd)))
                .andExpect(status().isAccepted())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void removeIncludeMembersTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/removeIncludeMembers";
        List<String> usersToRemove = new ArrayList<>();
        usersToRemove.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(put(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(usersToRemove)))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser
    public void removeExcludeMembersTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/removeExcludeMembers";
        List<String> usersToRemove = new ArrayList<>();
        usersToRemove.add(UID);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(put(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(usersToRemove)))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), anyList(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser
    public void ownerGroupingsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "owners/groupings";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void numberOfGroupingsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "owners/groupings/count";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void hasOwnerPrivsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "members/" + UID + "/is-owner";

        given(httpRequestService.makeApiRequest(anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void assignOwnershipTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/user/addOwnerships";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void addGroupPathOwnerships() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/groupPath/addGroupPathOwnerships";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void removeOwnershipsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/user/removeOwnerships";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser
    public void removeGroupPathOwnershipsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/groupPath/removeGroupPathOwnerships";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.DELETE)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.DELETE));
    }

    @Test
    @WithMockUhUser
    public void getGroupingTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "groupings/group?page=2&size=700&sortBy=name&isAscending=true";
        List<String> groupPaths = List.of(GROUPING);

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), eq(groupPaths), eq(HttpMethod.POST)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJson(groupPaths)))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), eq(groupPaths), eq(HttpMethod.POST));
    }

    @Test
    @WithMockUhUser
    public void updateDescriptionTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "groupings/path/description";

        given(httpRequestService.makeApiRequestWithBody(eq(UID), anyString(), nullable(String.class),
                eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(put(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequestWithBody(eq(UID), anyString(), nullable(String.class), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void updateSyncDestTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "groupings/" + GROUPING + "/syncDests/listserv/";

        String enableUri = uri + "true";
        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(enableUri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
        String disableUri = uri + "false";
        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(disableUri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(2))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }
    @Test
    @WithMockUhUser
    public void updateOptInTrueTest() throws Exception {
        String uri = REST_CONTROLLER_BASE +"groupings/"+ GROUPING + "/opt-attribute/IN/true";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void updateOptInFalseTest() throws Exception {
        String uri = REST_CONTROLLER_BASE +"groupings/"+ GROUPING + "/opt-attribute/IN/false";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void updateOptOutTrueTest() throws Exception {
        String uri = REST_CONTROLLER_BASE +"groupings/"+ GROUPING + "/opt-attribute/OUT/true";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void updateOptOutFalseTest() throws Exception {
        String uri = REST_CONTROLLER_BASE +"groupings/"+ GROUPING + "/opt-attribute/OUT/false";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(post(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.PUT));
    }

    @Test
    @WithMockUhUser
    public void getNumberOfOwners() throws Exception {
        String uri = REST_CONTROLLER_BASE + GROUPING + "/owners/" + UID + "/count";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void allSyncDestinationsTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "/groupings/" + GROUPING + "/sync-destinations";

        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.OK));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isOk())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void getAsyncJobResultTest() throws Exception {
        String uri = REST_CONTROLLER_BASE + "jobs/0";
        given(httpRequestService.makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET)))
                .willReturn(new ResponseEntity(HttpStatus.ACCEPTED));

        assertNotNull(mockMvc.perform(get(uri).with(csrf()))
                .andExpect(status().isAccepted())
                .andReturn());

        verify(httpRequestService, times(1))
                .makeApiRequest(eq(UID), anyString(), eq(HttpMethod.GET));
    }

    @Test
    @WithMockUhUser
    public void shouldDoGroupingsHandshake() throws Exception {
        GroupingsRestController controller = applicationContext.getBean(GroupingsRestController.class);

        Realm realm = controller.getRealm();
        assertFalse(realm.isAnyProfileActive("default"));
        assertTrue(realm.isAnyProfileActive("localTest"));

        // What we are testing.
        assertFalse(controller.shouldDoApiHandshake());

        Realm realmMock = mock(Realm.class);
        controller.setRealm(realmMock); // Swap in mock.
        assertTrue(mockingDetails(controller.getRealm()).isMock());
        given(realmMock.isAnyProfileActive("default", "localTest"))
                .willReturn(false);

        // What we are testing.
        assertTrue(controller.shouldDoApiHandshake());

        // Mock real realm back.
        controller.setRealm(realm);
        assertFalse(mockingDetails(controller.getRealm()).isMock());

        // Let's cheat a little to get at some private fields.
        Class<?> c0 = controller.getClass();
        Field field0 = c0.getDeclaredField("API_HANDSHAKE_ENABLED");
        field0.setAccessible(true);
        Boolean existingValue = (Boolean) field0.get(controller);
        assertTrue(existingValue);
        field0.set(controller, Boolean.FALSE);

        // What we are testing.
        assertFalse(controller.shouldDoApiHandshake());

        // Put property value back.
        field0.set(controller, Boolean.TRUE);
    }

    @Test
    @WithMockUhUser
    public void groupingsHandshake() {
        GroupingsRestController controller = applicationContext.getBean(GroupingsRestController.class);
        assertFalse(controller.shouldDoApiHandshake());
        try {
            controller.doApiHandshake();
        } catch (Exception e) {
            fail("Should not reach here.");
        }

        Realm realm = controller.getRealm();
        Realm realmMock = mock(Realm.class);
        controller.setRealm(realmMock); // Swap in mock.
        assertTrue(mockingDetails(controller.getRealm()).isMock());
        given(realmMock.isAnyProfileActive("default", "localTest"))
                .willReturn(false);

        try {
            controller.doApiHandshake();
            fail("Should not reach here.");
        } catch (Exception e) {
            assertThat(e, instanceOf(ApiServerHandshakeException.class));
        }

        // Cause in an internal exception.
        HttpRequestService httpRequestServiceOriginal = controller.getHttpRequestService();
        controller.setHttpRequestService(null);

        try {
            controller.doApiHandshake();
            fail("Should not reach here.");
        } catch (Exception e) {
            assertThat(e, instanceOf(ApiServerHandshakeException.class));
        }

        // Put stuff back.
        controller.setHttpRequestService(httpRequestServiceOriginal);
        controller.setRealm(realm);
        assertFalse(mockingDetails(controller.getRealm()).isMock());
    }

    @Test
    public void mapGroupingParametersTest() {
        Map<String, String> params = groupingsRestController.mapGroupingParameters(1, 2, "name", true);
        assertEquals(4, params.size());
        assertTrue(params.containsKey("page"));
        assertEquals("1", params.get("page"));
        assertTrue(params.containsKey("size"));
        assertEquals("2", params.get("size"));
        assertTrue(params.containsKey("sortBy"));
        assertEquals("name", params.get("sortBy"));
        assertTrue(params.containsKey("isAscending"));
        assertEquals("true", params.get("isAscending"));
    }

    @Test
    public void buildUriWithParamsTest() {
        Map<String, String> params = groupingsRestController.mapGroupingParameters(1, 2, "name", true);
        String uriTemplate = groupingsRestController.buildUriWithParams(API_2_1_BASE, params);
        assertNotNull(uriTemplate);
        String expectedResult = API_2_1_BASE + "?size=2&sortBy=name&page=1&isAscending=true";
        assertEquals(expectedResult, uriTemplate);
    }

    @Test
    public void sanitizeListTest() {
        List<String> listToSanitize = new ArrayList<>();
        listToSanitize.add(UID);
        listToSanitize.add("<a href='/foo?param1=1&param2=2'></a>");
        listToSanitize.add("<p style='color: red'></p>");
        listToSanitize.add("<img></img>");
        listToSanitize.add("<script></script>");

        List<String> sanitizedList = groupingsRestController.sanitizeList(listToSanitize);
        assertTrue(sanitizedList.contains(UID));
        assertFalse(sanitizedList.contains("<a href='/foo?param1=1&param2=2'></a>"));
        assertFalse(sanitizedList.contains("<p style='color: red'></p>"));
        assertFalse(sanitizedList.contains("<img></img>"));
        assertFalse(sanitizedList.contains("<script></script>"));
    }
}