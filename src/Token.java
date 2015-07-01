
public class Token {

	private String gloss;

	private String synsetId, tokenSynsetId;
	private String lemma, pos, glossId, language;
	private int position;
	
	public Token(String synsetId,String glossId, String gloss) {
		this.synsetId = synsetId;
		this.glossId = glossId;
		this.gloss = gloss;
	}

	public String getGloss() {
		return gloss;
	}

	public String getGlossId() {
		return glossId;
	}

	public String getLanguage() {
		return language;
	}

	public void setGloss(String gloss) {
		this.gloss = gloss;
	}

	public void setGlossId(String glossId) {
		this.glossId = glossId;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Token(String synsetId, String lemma, String pos, String lang, String glossId, int position) {
		this.synsetId = synsetId;
		this.lemma = lemma;
		this.pos = pos;
		this.position = position;
		this.glossId = glossId;
		this.language = lang;
	}

	public String getSynsetId() {
		return synsetId;
	}

	public String getTokenSynsetId() {
		return tokenSynsetId;
	}

	public String getLemma() {
		return lemma;
	}

	public String getPos() {
		return pos;
	}

	public int getPosition() {
		return position;
	}

	public void setSynsetId(String synsetId) {
		this.synsetId = synsetId;
	}

	public void setTokenSynsetId(String tokenSynsetId) {
		this.tokenSynsetId = tokenSynsetId;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	private void fillFields(){
		String[] tokens = gloss.split("\\s+");
		for(int i = 0 ; i < tokens.length ; ++i)
		{
			String t = tokens[i];
			int beginIndex = t.indexOf("$");
			String pos = t.substring(beginIndex + 1);
			String word = t.substring(0, beginIndex).replaceAll("\\W", "");
			System.out.println(word  + " [" + pos + "]");
		}
	}
	
}
