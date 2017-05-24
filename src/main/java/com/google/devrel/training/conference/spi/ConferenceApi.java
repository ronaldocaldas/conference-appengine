package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.factory;
import static com.google.devrel.training.conference.service.OfyService.ofy;

import java.util.List;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", 
		version = "v1", 
		scopes = { Constants.EMAIL_SCOPE }, 
		clientIds = { Constants.WEB_CLIENT_ID,
		Constants.API_EXPLORER_CLIENT_ID }, 
		description = "API for the Conference Central Backend application.")
public class ConferenceApi {

	/*
	 * Get the display name from the user's email. For example, if the email is
	 * lemoncake@example.com, then the display name becomes "lemoncake."
	 */
	private static String extractDefaultDisplayNameFromEmail(String email) {
		return email == null ? null : email.substring(0, email.indexOf("@"));
	}

	/**
	 * Creates or updates a Profile object associated with the given user
	 * object.
	 *
	 * @param user
	 *            A User object injected by the cloud endpoints.
	 * @param profileForm
	 *            A ProfileForm object sent from the client form.
	 * @return Profile object just created.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */

	// Declare this method as a method available externally through Endpoints
	@ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
	// The request that invokes this method should provide data that
	// conforms to the fields defined in ProfileForm
	
	public Profile saveProfile(final User user, ProfileForm profileForm) throws UnauthorizedException {

		// If the user is not logged in, throw an UnauthorizedException
		if (user == null) {
			throw new UnauthorizedException("Autorization required");
		}

		// Get the userId and mainEmail
		String mainEmail = user.getEmail();
		String userId = user.getUserId();

		// Get the displayName and teeShirtSize sent by the request
		String displayName = profileForm.getDisplayName();
		TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();

		// Create a new Profile entity from the
		// userId, displayName, mainEmail and teeShirtSize
		Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

		if (profile == null) {
			// populate the dysplayName and teeShirtSize with default values
			// if not send the request
			if (displayName == null) {
				displayName = extractDefaultDisplayNameFromEmail(user.getEmail());
			}
			if (teeShirtSize == null) {
				teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
			}

			profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
		} else {
			profile.update(displayName, teeShirtSize);
		}

		// Save the Profile entity in the datastore
		ofy().save().entity(profile).now();
		// Return the profile
		return profile;
	}

	/**
	 * Returns a Profile object associated with the given user object. The cloud
	 * endpoints system automatically inject the User object.
	 *
	 * @param user
	 *            A User object injected by the cloud endpoints.
	 * @return Profile object.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */
	@ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
	public Profile getProfile(final User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		// load the Profile Entity
		String userId = user.getUserId();
		Key key = Key.create(Profile.class, userId);
		Profile profile = (Profile) ofy().load().key(key).now(); // load the
																	// Profile
																	// entity
		return profile;
	}
	
	
	/**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId of the logged in User
        String userId = user.getUserId();

        // Get the key for the User's Profile
        Key<Profile> profileKey = factory().allocateId(Profile.class);

        // Allocate a key for the conference -- let App Engine allocate the ID
        // Don't forget to include the parent Profile in the allocated ID
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        // Get the Conference Id from the Key
        final long conferenceId = conferenceKey.getId();

        // Get the existing Profile entity for the current user if there is one
        // Otherwise create a new Profile entity with default values
         Profile profile = getProfile(user);
         
         if (profile == null) {
 			profile = new Profile(userId, extractDefaultDisplayNameFromEmail(user.getEmail()), user.getEmail(), TeeShirtSize.NOT_SPECIFIED);
 		} 
         

        // Create a new Conference Entity, specifying the user's Profile entity
        // as the parent of the conference
        Conference conference = new Conference(conferenceId, userId, conferenceForm);

        // Save Conference and Profile Entities
         ofy().save().entities(conference,profile).now();

         return conference;
         }
    
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences() {
    	//find all entities of type conference
        Query query = ofy().load().type(Conference.class).order("name");
        return query.list();
    }
    
    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user) throws UnauthorizedException {
    	
    	if(user == null){
    		throw new UnauthorizedException("Authorization required");
    	}
    	String userId = user.getUserId();
    	Key userKey = Key.create(Profile.class, userId);
    	
    	return ofy().load().type(Conference.class).ancestor(userKey).order("name").list();
    }
    public List<Conference> filterPlayground(){
    	Query<Conference> query = ofy().load().type(Conference.class).order("name");
    	query = query.filter("city=", "London");
    	query = query.filter("topics=", "Medical Innovations");
    	return query.list();
    	
    }
}
