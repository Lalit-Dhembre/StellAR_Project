import os
import logging
from appwrite.client import Client
from appwrite.services.databases import Databases
from appwrite.services.storage import Storage
from appwrite.query import Query
from appwrite.id import ID
from appwrite.input_file import InputFile
from appwrite.permission import Permission
from appwrite.role import Role
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)


class AppwriteService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(AppwriteService, cls).__new__(cls)
            cls._instance.client = None
            cls._instance.databases = None
            cls._instance.storage = None
            cls._instance.initialized = False
            cls._instance.database_id = None
        return cls._instance

    def initialize(self):
        if self.initialized:
            return

        endpoint = os.environ.get("APPWRITE_ENDPOINT")
        project_id = os.environ.get("APPWRITE_PROJECT_ID")
        api_key = os.environ.get("APPWRITE_API")
        self.database_id = os.environ.get("APPWRITE_DATABASE_ID")

        if not endpoint or not project_id or not api_key:
            logger.warning("⚠️ APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID, or APPWRITE_API not found in environment variables. Tailored features will be disabled.")
            return

        try:
            self.client = Client()
            self.client.set_endpoint(endpoint)
            self.client.set_project(project_id)
            self.client.set_key(api_key)

            self.databases = Databases(self.client)
            self.storage = Storage(self.client)
            self.initialized = True
            logger.info("✅ Appwrite client initialized successfully")
        except Exception as e:
            logger.error(f"❌ Failed to initialize Appwrite client: {e}")

    def get_client(self):
        if not self.initialized:
            self.initialize()
        return self.client

    def upload_file(self, bucket: str, file_path: str, destination_path: str) -> str:
        """
        Uploads a file to Appwrite Storage and returns the public view URL.
        """
        if not self.initialized:
            raise Exception("Appwrite not initialized")

        try:
            # Use the destination_path as a unique file ID (sanitized)
            file_id = ID.unique()

            result = self.storage.create_file(
                bucket_id=bucket,
                file_id=file_id,
                file=InputFile.from_path(file_path),
                permissions=[Permission.read(Role.any())]
            )

            # Construct the public view URL
            endpoint = os.environ.get("APPWRITE_ENDPOINT")
            project_id = os.environ.get("APPWRITE_PROJECT_ID")
            public_url = f"{endpoint}/storage/buckets/{bucket}/files/{result['$id']}/view?project={project_id}"
            return public_url
        except Exception as e:
            logger.error(f"Failed to upload to Appwrite: {e}")
            raise e

    def insert_record(self, table: str, data: dict):
        """Insert a document into an Appwrite collection."""
        if not self.initialized:
            raise Exception("Appwrite not initialized")

        try:
            result = self.databases.create_document(
                database_id=self.database_id,
                collection_id=table,
                document_id=ID.unique(),
                data=data,
                permissions=[Permission.read(Role.any())]
            )
            return result
        except Exception as e:
            logger.error(f"Failed to insert record into {table}: {e}")
            raise e

    def update_record(self, table: str, match: dict, update: dict):
        """Update documents matching the given filters."""
        if not self.initialized:
            raise Exception("Appwrite not initialized")

        try:
            # First find documents that match
            queries = [Query.equal(key, value) for key, value in match.items()]
            docs = self.databases.list_documents(
                database_id=self.database_id,
                collection_id=table,
                queries=queries
            )

            results = []
            for doc in docs['documents']:
                updated = self.databases.update_document(
                    database_id=self.database_id,
                    collection_id=table,
                    document_id=doc['$id'],
                    data=update
                )
                results.append(updated)

            return results
        except Exception as e:
            logger.error(f"Failed to update record in {table}: {e}")
            raise e

    def query_records(self, table: str, select: str = "*", filters: dict = None):
        """Query documents from an Appwrite collection."""
        if not self.initialized:
            self.initialize()
            if not self.initialized:
                return []

        try:
            queries = []
            if filters:
                for key, value in filters.items():
                    queries.append(Query.equal(key, value))

            if select and select != "*":
                # Appwrite uses Query.select() for field selection
                fields = [f.strip() for f in select.split(",")]
                queries.append(Query.select(fields))

            response = self.databases.list_documents(
                database_id=self.database_id,
                collection_id=table,
                queries=queries if queries else None
            )

            return response['documents']
        except Exception as e:
            logger.error(f"Failed to query {table}: {e}")
            return []


# Global instance
appwrite_service = AppwriteService()
