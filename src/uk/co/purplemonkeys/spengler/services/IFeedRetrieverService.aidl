package uk.co.purplemonkeys.spengler.services;

interface IFeedRetrieverService {
	void retrieveURI(in String uri);
	void retrieveFeed(long id);
	void retrieveAllFeeds();
}