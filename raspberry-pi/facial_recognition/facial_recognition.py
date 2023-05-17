from collections import Counter
import os
import face_recognition
from pathlib import Path
import pickle
import shutil
import sys

sys.path.append('.')

from storage_accessor.StorageAccessor import StorageAccessor


DEFAULT_ENCODINGS_PATH = Path('output/encodings.pkl')

Path('output').mkdir(exist_ok=True)


def encode_new_faces(
    name: str, model: str = 'hog', encodings_location: Path = DEFAULT_ENCODINGS_PATH
) -> None:
    StorageAccessor.save_then_delete_blobs(name)

    names = []
    encodings = []

    for file_path in os.listdir(name):
        image = face_recognition.load_image_file(os.path.join(name, file_path))

        face_locations = face_recognition.face_locations(image, model=model)
        face_encodings = face_recognition.face_encodings(image, face_locations)

        for encoding in face_encodings:
            names.append(name)
            encodings.append(encoding)

    shutil.rmtree(name)

    name_encodings = { 'names': names, 'encodings': encodings }

    if (Path.exists(encodings_location)):
        with encodings_location.open(mode='rb') as f:
            loaded_encodings = pickle.load(f)
            name_encodings['names'].extend(loaded_encodings['names'])
            name_encodings['encodings'].extend(loaded_encodings['encodings'])
      
    with encodings_location.open(mode='wb') as f:
        pickle.dump(name_encodings, f)


def remove_encoded_faces(name: str, encodings_location: Path = DEFAULT_ENCODINGS_PATH):
    if (not Path.exists(encodings_location)):
        return
    
    f = encodings_location.open(mode='rb')
    loaded_encodings = pickle.load(f)
    f.close()
    
    filtered = list(filter(lambda x: x[0] != name, zip(loaded_encodings['names'], loaded_encodings['encodings'])))
    loaded_encodings = { 'names': [i[0] for i in filtered], 'encodings': [i[1] for i in filtered] }

    with encodings_location.open(mode='wb') as f:
        pickle.dump(loaded_encodings, f)


def recognize_faces(
    image_location: str,
    model: str = 'hog',
    encodings_location: Path = DEFAULT_ENCODINGS_PATH,
) -> None:
    with encodings_location.open(mode='rb') as f:
        loaded_encodings = pickle.load(f)

    input_image = face_recognition.load_image_file(image_location)

    input_face_locations = face_recognition.face_locations(
        input_image, model=model
    )
    input_face_encodings = face_recognition.face_encodings(
        input_image, input_face_locations
    )

    for bounding_box, unknown_encoding in zip(
        input_face_locations, input_face_encodings
    ):
        name = __recognize_face(unknown_encoding, loaded_encodings)
        if not name:
            name = 'Unknown'
        print(name, bounding_box)


def __recognize_face(unknown_encoding, loaded_encodings):
    boolean_matches = face_recognition.compare_faces(
        loaded_encodings['encodings'], unknown_encoding
    )
    votes = Counter(
        name
        for match, name in zip(boolean_matches, loaded_encodings['names'])
        if match
    )
    if votes:
        return votes.most_common(1)[0][0]