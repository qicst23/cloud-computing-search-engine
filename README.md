MiniGoogle - Cloud Computing Search Egine
=============================

## About
* A web search engine computing Hadoop MapReduce on Amazon EC2 consisting of crawler, indexer, PageRank.
* CIS 555, Internet & Web Systems, Spring 2013, University of Pennsylvania
* Yayang Tian, Michael Collis, Angela Wu, Krishna Choksi

## Contribution
* Developed a scalable, Google-style crawler that distributed requests across multiple crawling peers over Pastry nodes. 
* Developed a TF-IDF indexer for inverted index computation and a PageRank engine for link analysis based on MapReduce. 
* Improved search relevancy by weighting ten ranking parameters, utilizing AJAX feedback and SVM classifier for tuning.
* Implemented features for fault tolerance with Berkeley DB revert, page preview, safe search, Yahoo, Amazon, YouTube API.
